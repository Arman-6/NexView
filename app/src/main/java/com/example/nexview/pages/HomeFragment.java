package com.example.nexview.pages;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nexview.R;
import com.example.nexview.youtube.VideoAdapter;
import com.example.nexview.youtube.VideoModel;
import com.example.nexview.youtube.YouTubeApiService;
import com.example.nexview.youtube.YouTubeResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class HomeFragment extends Fragment implements VideoAdapter.OnVideoClickListener{

    private RecyclerView recyclerView;
    private VideoAdapter videoAdapter;
    private List<VideoModel> videoList;
    private boolean isLoading = false;
    private ProgressBar progressBar;
    private String apiKey;
    private String userTopic;
    private YouTubeApiService youTubeApiService;
    private int currentKeyIndex = 0;
    private int apiKeyAttempts = 0;
    private static final int VIDEO_LIMIT = 10; // Increased to get more variety
    private static final String TAG = "HomeFragment";
    private static final String KEY_INDEX_PREF = "current_api_key_index";
    private static final int MAX_KEY_ROTATION_ATTEMPTS = 10; // Prevent infinite rotation

    // Track loaded video IDs to prevent duplicates
    private Set<String> loadedVideoIds = new HashSet<>();

    // Track pagination tokens
    private String nextPageToken = null;

    // Track different query variations to get diverse results
    private String[] queryVariations;
    private int currentQueryIndex = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userTopic = sharedPreferences.getString("selectedTopic", "Technology");

        // Create query variations
        createQueryVariations();

        // Load the last used API key index
        currentKeyIndex = sharedPreferences.getInt(KEY_INDEX_PREF, 0);
        Log.d(TAG, "Starting with API key index: " + currentKeyIndex);

        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        videoList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        videoAdapter = new VideoAdapter(getContext(), videoList, this);
        recyclerView.setAdapter(videoAdapter);

        youTubeApiService = createYouTubeApiService();

        // Get a valid API key
        apiKey = getValidApiKey();
        if (apiKey != null) {
            // Load initial videos
            fetchVideos();
        } else {
            Toast.makeText(getContext(), "All API quotas exhausted. Try again later.", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            isLoading = false;
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (!isLoading && layoutManager != null &&
                        layoutManager.findLastVisibleItemPosition() >= videoList.size() - 3) {
                    // Load more videos when approaching the end of the list
                    fetchVideos();
                }
            }
        });
        return view;
    }

    /*
         Creating variations of the query to get diverse results
     */
    private void createQueryVariations() {
        queryVariations = new String[]{
                userTopic + " tutorials",
                userTopic + " best",
                userTopic + " review",
                userTopic + " latest",
                userTopic + " tips",
                userTopic + " explained",
                userTopic + " guide",
                userTopic + " advanced",
                userTopic
        };
    }

    /**
     * Get the next query variation
     */
    private String getNextQueryVariation() {
        String query = queryVariations[currentQueryIndex];
        currentQueryIndex = (currentQueryIndex + 1) % queryVariations.length;
        return query;
    }

    private void fetchVideos() {
        if (isLoading) {
            return;
        }

        if (!progressBar.isShown()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        isLoading = true;

        // Use different ordering to get diverse results
        String[] orderTypes = {"date", "viewCount", "rating", "relevance"};
        String randomOrder = orderTypes[new Random().nextInt(orderTypes.length)];

        // Get next query variation
        String query = getNextQueryVariation();
        Log.d(TAG, "Fetching videos with query: " + query + ", order: " + randomOrder);

        Call<YouTubeResponse> call = youTubeApiService.getVideos(
                "snippet", VIDEO_LIMIT, query, "video",
                randomOrder, apiKey, nextPageToken
        );

        call.enqueue(new Callback<YouTubeResponse>() {
            @Override
            public void onResponse(@NonNull Call<YouTubeResponse> call, @NonNull Response<YouTubeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Save next page token for pagination
                    nextPageToken = response.body().getNextPageToken();

                    List<VideoModel> newVideos = response.body().getVideos();
                    Log.d(TAG, "Received " + (newVideos != null ? newVideos.size() : 0) + " videos");

                    if (newVideos != null && !newVideos.isEmpty()) {
                        // Filter shorts and duplicates
                        List<VideoModel> filteredVideos = filterDuplicatesAndShorts(newVideos);

                        if (!filteredVideos.isEmpty()) {
                            // Shuffle to get variety in display order
                            Collections.shuffle(filteredVideos);
                            videoList.addAll(filteredVideos);
                            videoAdapter.notifyDataSetChanged();
                            Log.d(TAG, "Added " + filteredVideos.size() + " new videos. Total: " + videoList.size());
                        } else {
                            Log.d(TAG, "All videos were filtered out or were duplicates");
                            // If all videos were filtered out, try fetching more with next variation
                            resetLoadingState();
                            fetchVideos();
                        }
                    } else {
                        Log.d(TAG, "Response successful but no videos returned");
                        // Try with next variation
                        resetLoadingState();
                        fetchVideos();
                    }
                    // Reset attempt counter on success
                    apiKeyAttempts = 0;
                    resetLoadingState();
                } else if (response.code() == 403) {
                    Log.e(TAG, "Quota Exceeded: Switching API Key from index " + currentKeyIndex);
                    apiKeyAttempts++;

                    if (apiKeyAttempts < MAX_KEY_ROTATION_ATTEMPTS) {
                        switchToNextApiKey();
                        // Only retry if we successfully got a new API key
                        if (apiKey != null) {
                            Log.d(TAG, "Retrying with new API key at index " + currentKeyIndex);
                            fetchVideos(); // Retry with the next API key
                        } else {
                            handleAllKeysExhausted();
                        }
                    } else {
                        handleTooManyKeyAttempts();
                    }
                } else {
                    Log.e(TAG, "Error response: " + response.code());
                    Toast.makeText(getContext(), "Error fetching videos: " + response.code(), Toast.LENGTH_SHORT).show();
                    resetLoadingState();
                }
            }

            @Override
            public void onFailure(@NonNull Call<YouTubeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to Fetch Videos", t);
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                resetLoadingState();
            }
        });
    }

    /**
     * Filter out shorts and duplicate videos
     */
    private List<VideoModel> filterDuplicatesAndShorts(List<VideoModel> videos) {
        List<VideoModel> filteredVideos = new ArrayList<>();
        for (VideoModel video : videos) {
            String videoId = video.getVideoId();
            String title = video.getTitle();

            if (videoId != null && title != null) {
                String lowerTitle = title.toLowerCase();
                // Only add videos that:
                // 1. Are not already in our list (check by ID)
                // 2. Don't have "shorts" in the title
                // 3. Don't have "#" in the title (often shorts/reels)
                if (!loadedVideoIds.contains(videoId) &&
                        !lowerTitle.contains("shorts") &&
                        !lowerTitle.contains("#")) {

                    // Add to our filtered list
                    filteredVideos.add(video);

                    // Add to our tracking set
                    loadedVideoIds.add(videoId);
                }
            }
        }
        return filteredVideos;
    }

    /**
     * Reset loading state and hide progress bar
     */
    private void resetLoadingState() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        isLoading = false;
    }

    /**
     * Handle the case when all API keys are exhausted
     */
    private void handleAllKeysExhausted() {
        Log.e(TAG, "All API Keys Exhausted");
        if (getContext() != null) {
            Toast.makeText(getContext(), "All API quotas exhausted. Try again later.", Toast.LENGTH_LONG).show();
        }
        resetLoadingState();

        // Reset the key index to 0 for next app launch
        if (getContext() != null) {
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(KEY_INDEX_PREF, 0); // Reset for next time
            editor.apply();
        }
    }

    /**
     * Handle the case when too many key rotation attempts have been made
     */
    private void handleTooManyKeyAttempts() {
        Log.e(TAG, "Too many key rotation attempts. Giving up.");
        if (getContext() != null) {
            Toast.makeText(getContext(), "Unable to find a working API key. Try again later.", Toast.LENGTH_LONG).show();
        }
        resetLoadingState();

        // Reset the key index to 0 for next app launch
        if (getContext() != null) {
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(KEY_INDEX_PREF, 0); // Reset for next time
            editor.apply();
        }
    }

    private YouTubeApiService createYouTubeApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.googleapis.com/youtube/v3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(YouTubeApiService.class);
    }

    @Override
    public void onVideoClick(VideoModel video) {
        Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
        intent.putExtra("videoId", video.getVideoId());
        intent.putExtra("videoTitle", video.getTitle());
        startActivity(intent);
    }

    /**
     * Switches to the next available API key and updates the saved index
     */
    private void switchToNextApiKey() {
        currentKeyIndex++;
        apiKey = getValidApiKey();

        // Save the current index to SharedPreferences
        if (getContext() != null) {
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(KEY_INDEX_PREF, currentKeyIndex);
            editor.apply();
            Log.d(TAG, "Saved new API key index: " + currentKeyIndex);
        }
    }

    private String getValidApiKey() {
        String[] apiKeys = getResources().getStringArray(R.array.youtube_api_keys);

        if (currentKeyIndex >= apiKeys.length) {
            Log.e(TAG, "All API Keys Exhausted. Total keys: " + apiKeys.length);
            return null;
        }

        String key = apiKeys[currentKeyIndex];
        Log.d(TAG, "Using API Key at index " + currentKeyIndex + ": " + key.substring(0, Math.min(5, key.length())) + "...");
        return key;
    }

    @Override
    public void onResume() {
        super.onResume();

        // If we have no videos yet and we're not loading, try to fetch videos
        if (videoList.isEmpty() && !isLoading && apiKey != null) {
            Log.d(TAG, "onResume: No videos loaded yet, trying to fetch");
            fetchVideos();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Make sure loading state is reset when fragment is paused
        if (isLoading) {
            resetLoadingState();
        }
    }
}