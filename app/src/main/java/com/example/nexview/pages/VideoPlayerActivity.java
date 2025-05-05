package com.example.nexview.pages;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nexview.R;
import com.example.nexview.youtube.VideoAdapter;
import com.example.nexview.PomodoroTimerService;
import com.example.nexview.youtube.VideoModel;
import com.example.nexview.youtube.YouTubeApiService;
import com.example.nexview.youtube.YouTubeResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class VideoPlayerActivity extends AppCompatActivity {
    private String videoId, videoTitle, userId;
    private FirebaseFirestore db;
    private YouTubePlayer youTubePlayer;
    private float lastTimestamp = 0f;
    private TextView title;

    private RecyclerView recyclerView;
    private VideoAdapter videoAdapter;
    private List<VideoModel> videoList;
    private boolean isLoading = false;
    private ProgressBar progressBar;
    private String apiKey;
    private String userTopic;
    private YouTubeApiService youTubeApiService;
    private ImageView likeButton, dislikeButton, saveButton, shareButton;
    private static final int VIDEO_LIMIT = 5;
    private int currentKeyIndex = 0;
    private BottomNavigationView bottomNavigationView;
    private boolean isPlaying = false;

    private int apiKeyAttempts = 0;
    private static final String TAG = "HomeFragment";
    private static final String KEY_INDEX_PREF = "current_api_key_index";
    private static final int MAX_KEY_ROTATION_ATTEMPTS = 10; // Prevent infinite rotation
    private boolean isLiked = false;
    private boolean isDisliked = false;
    private boolean isSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get user ID if logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            if (userId != null) {
                userId = userId.replace(".", "");
            } else {
                Toast.makeText(this, "User email is null", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Load SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userTopic = sharedPreferences.getString("selectedTopic", "Technology");

        // Hide status bar and make full-screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_video_player);

        bottomNavigationView = findViewById(R.id.bottomNav);

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    Intent intent = new Intent(VideoPlayerActivity.this, MainActivity.class);
                    intent.putExtra("navigateTo", "HomeFragment"); // "targetFragment" is your fragment name
                    startActivity(intent);
                    finish();
                } else if (itemId == R.id.nav_profile) {
                    Intent intent = new Intent(VideoPlayerActivity.this, MainActivity.class);
                    intent.putExtra("navigateTo", "ProfileFragment"); // "targetFragment" is your fragment name
                    startActivity(intent);
                    finish();
                } else if (itemId == R.id.nav_history) {
                    Intent intent = new Intent(VideoPlayerActivity.this, MainActivity.class);
                    intent.putExtra("navigateTo", "HistoryFragment"); // "targetFragment" is your fragment name
                    startActivity(intent);
                    finish();
                } else if (itemId == R.id.nav_like) {
                    Intent intent = new Intent(VideoPlayerActivity.this, MainActivity.class);
                    intent.putExtra("navigateTo", "LikedFragment"); // "targetFragment" is your fragment name
                    startActivity(intent);
                    finish();
                }
                return true;
            }
        });

        db = FirebaseFirestore.getInstance();

        // Initialize Views
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recommendationRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        videoList = new ArrayList<>();
        videoAdapter = new VideoAdapter(this, videoList, videoModel -> {
            Intent intent = new Intent(VideoPlayerActivity.this, VideoPlayerActivity.class);
            intent.putExtra("videoId", videoModel.getVideoId());
            intent.putExtra("videoTitle", videoModel.getTitle());
            startActivity(intent);
        });
        recyclerView.setAdapter(videoAdapter);

        // Retrieve video details from intent
        videoId = getIntent().getStringExtra("videoId");
        videoTitle = getIntent().getStringExtra("videoTitle");

        if (videoId == null || videoId.isEmpty()) {
            Toast.makeText(this, "Invalid Video ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (videoTitle == null) {
            videoTitle = "Unknown Video";
        }

        title = findViewById(R.id.Title);
        title.setText(Html.fromHtml(videoTitle, Html.FROM_HTML_MODE_LEGACY));

        // Initialize YouTube Player
        YouTubePlayerView youTubePlayerView = findViewById(R.id.youtubePlayerView);
        getLifecycle().addObserver(youTubePlayerView);

        fetchLastWatchTimestamp(userTopic);
        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer player) {
                youTubePlayer = player;
                youTubePlayer.loadVideo(videoId, lastTimestamp);
                saveVideoWatchHistory(userTopic);
                // Start the timer when video is ready
                isPlaying = true;
                startTimerService();
            }

            @Override
            public void onCurrentSecond(@NonNull YouTubePlayer player, float second) {
                lastTimestamp = second;
                updateTimestamp(userTopic);
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer player, @NonNull PlayerConstants.PlayerState state) {
                if (state == PlayerConstants.PlayerState.PLAYING) {
                    isPlaying = true;
                    startTimerService();
                }
//                else if (state == PlayerConstants.PlayerState.PAUSED ||
//                        state == PlayerConstants.PlayerState.ENDED) {
//                    isPlaying = false;
//                    updateTimerService();
//                }
            }
        });

        youTubeApiService = createYouTubeApiService();
        apiKey = "AIzaSyC0dLnkIuLRBYe2_s3q_1Yd_gdhQlAL3L0";

        if (apiKey != null) {
            fetchVideos();
        } else {
            Toast.makeText(getApplicationContext(), "API Quota Exhausted. Try again later.", Toast.LENGTH_LONG).show();
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (!isLoading && layoutManager != null && !videoList.isEmpty()){
                    fetchVideos();
                }
            }
        });

        likeButton = findViewById(R.id.likeButton);
        saveButton = findViewById(R.id.saveButton);
        shareButton = findViewById(R.id.shareButton);
        dislikeButton = findViewById(R.id.dislikeButton);

        // Check if video is already liked or disliked when loading the page
        checkVideoStatus(userTopic);

        // Replace your existing onClick listeners with these:
        likeButton.setOnClickListener(v -> toggleLikeStatus(userTopic));
        dislikeButton.setOnClickListener(v -> toggleDislikeStatus(userTopic));
        saveButton.setOnClickListener(v -> toggleSaveStatus(userTopic));
        shareButton.setOnClickListener(v -> shareVideo());


        ImageButton searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(VideoPlayerActivity.this, SearchFragment.class);
                startActivity(intent);
            }
        });

    }

    public void fetchLastWatchTimestamp(String sessionName) {
        db.collection("users").document(userId)
                .collection("sessions").document(sessionName)
                .collection("watchHistory").document(sanitizeTitle(videoTitle))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        lastTimestamp = documentSnapshot.getDouble("timestamp") != null ?
                                documentSnapshot.getDouble("timestamp").floatValue() : 0f;
                    }
                });
    }

    private void saveVideoWatchHistory(String sessionName) {
        Map<String, Object> videoData = new HashMap<>();
        videoData.put("title", videoTitle);
        videoData.put("videoId", videoId);
        videoData.put("timestamp", lastTimestamp);
        videoData.put("sequence", FieldValue.serverTimestamp()); // Add server timestamp


        db.collection("users").document(userId)
                .collection("sessions").document(sessionName)
                .collection("watchHistory").document(sanitizeTitle(videoTitle))
                .set(videoData, SetOptions.merge());
    }

    private void updateTimestamp(String sessionName) {
        db.collection("users").document(userId)
                .collection("sessions").document(sessionName)
                .collection("watchHistory").document(sanitizeTitle(videoTitle))
                .update("timestamp", lastTimestamp);
    }

    private YouTubeApiService createYouTubeApiService() {
        return new Retrofit.Builder()
                .baseUrl("https://www.googleapis.com/youtube/v3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(YouTubeApiService.class);
    }

    private void shareVideo() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "From NexView\n Watch this Video: https://www.youtube.com/watch?v=" + videoId);
        startActivity(Intent.createChooser(shareIntent, "Share video via"));
    }


    private void fetchVideos() {
        if (isLoading && !progressBar.isShown()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        isLoading = true;

        // Generate a random character to modify the query
        char randomChar = (char) ('A' + new Random().nextInt(26)); // A-Z
        String randomQuery = userTopic + " " + randomChar;

        // Random order type
        String[] orderTypes = {"date", "viewCount", "rating","title","relevance"};
        String randomOrder = orderTypes[new Random().nextInt(orderTypes.length)];

        Call<YouTubeResponse> call = youTubeApiService.getVideos(
                "snippet", VIDEO_LIMIT, randomQuery, "video",
                randomOrder, apiKey, ""
        );

        call.enqueue(new Callback<YouTubeResponse>() {
            @Override
            public void onResponse(@NonNull Call<YouTubeResponse> call, @NonNull Response<YouTubeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<VideoModel> newVideos = response.body().getVideos();

                    if (newVideos != null && !newVideos.isEmpty()) {
                        List<VideoModel> filteredVideos = filterOutShorts(newVideos);
                        if (!filteredVideos.isEmpty()) {
                            Collections.shuffle(filteredVideos);
                            videoList.addAll(filteredVideos);
                            videoAdapter.notifyDataSetChanged();
                            resetLoadingState();
                        }
                        // Reset attempt counter on success
                        apiKeyAttempts = 0;
                    } else {
                        Log.d(TAG, "Response successful but no videos returned");
                        resetLoadingState();
                    }
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
                    Toast.makeText(getApplicationContext(), "Error fetching videos: " + response.code(), Toast.LENGTH_SHORT).show();
                    resetLoadingState();
                }
            }

            @Override
            public void onFailure(@NonNull Call<YouTubeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to Fetch Videos", t);
                Toast.makeText(getApplicationContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                resetLoadingState();
            }
        });
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
        if (getApplicationContext() != null) {
            Toast.makeText(getApplicationContext(), "All API quotas exhausted. Try again later.", Toast.LENGTH_LONG).show();
        }
        resetLoadingState();

        // Reset the key index to 0 for next app launch
        if (getApplicationContext() != null) {
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
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
        if (getApplicationContext() != null) {
            Toast.makeText(getApplicationContext(), "Unable to find a working API key. Try again later.", Toast.LENGTH_LONG).show();
        }
        resetLoadingState();

        // Reset the key index to 0 for next app launch
        if (getApplicationContext() != null) {
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(KEY_INDEX_PREF, 0); // Reset for next time
            editor.apply();
        }
    }

    private String sanitizeTitle(String title) {
        return title.replaceAll("[^a-zA-Z0-9]", "_"); // Replace special characters with "_"
    }
    // Methods to control the timer service
    private void startTimerService() {
        Intent serviceIntent = new Intent(this, PomodoroTimerService.class);
        serviceIntent.putExtra("isVideoPlaying", true);
        serviceIntent.putExtra("mailId", userId);
        serviceIntent.putExtra("sessionName", userTopic);
        startService(serviceIntent);
        Log.d("VideoPlayerActivity", "Starting timer service with mailId: " + userId + ", session: " + userTopic);
    }

    private List<VideoModel> filterOutShorts(List<VideoModel> videos) {
        List<VideoModel> filteredVideos = new ArrayList<>();
        for (VideoModel video : videos) {
            String title = video.getTitle();
            if (title != null) {
                String lowerTitle = title.toLowerCase();
                if (!lowerTitle.contains("shorts") && !lowerTitle.contains("#")) {
                    filteredVideos.add(video);
                }
            }
        }
        return filteredVideos;
    }
    private void switchToNextApiKey() {
        currentKeyIndex++;
        apiKey = getValidApiKey();

        // Save the current index to SharedPreferences
        if (getApplicationContext() != null) {
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
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

    private void checkVideoStatus(String sessionName) {
        // Check if video is liked
        db.collection("users").document(userId)
                .collection("sessions").document(sessionName)
                .collection("likeVideos").document(sanitizeTitle(videoTitle))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        isLiked = true;
                        likeButton.setImageResource(R.drawable.liked); // Add this drawable
                    } else {
                        isLiked = false;
                        likeButton.setImageResource(R.drawable.like); // Add this drawable
                    }
                });

        // Check if video is disliked
        db.collection("users").document(userId)
                .collection("sessions").document(sessionName)
                .collection("dislikeVideos").document(sanitizeTitle(videoTitle))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        isDisliked = true;
                        dislikeButton.setImageResource(R.drawable.disliked); // Add this drawable
                    } else {
                        isDisliked = false;
                        dislikeButton.setImageResource(R.drawable.dislike); // Add this drawable
                    }
                });

        // Check if video is saved
        db.collection("users").document(userId)
                .collection("sessions").document(sessionName)
                .collection("saveVideos").document(sanitizeTitle(videoTitle))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        isSaved = true;
                        saveButton.setImageResource(R.drawable.saved); // Add this drawable
                    } else {
                        isSaved = false;
                        saveButton.setImageResource(R.drawable.save); // Add this drawable
                    }
                });
    }

    // Add these toggle methods
    private void toggleLikeStatus(String sessionName) {
        if (isLiked) {
            // Update UI immediately
            isLiked = false;
            likeButton.setImageResource(R.drawable.like);
            Toast.makeText(this, "Removed from likes", Toast.LENGTH_SHORT).show();

            // Then update database
            db.collection("users").document(userId)
                    .collection("sessions").document(sessionName)
                    .collection("likeVideos").document(sanitizeTitle(videoTitle))
                    .delete()
                    .addOnFailureListener(e -> {
                        // Rollback UI if operation fails
                        isLiked = true;
                        likeButton.setImageResource(R.drawable.liked);
                        Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Update UI immediately
            isLiked = true;
            likeButton.setImageResource(R.drawable.liked);
            Toast.makeText(this, "Added to likes", Toast.LENGTH_SHORT).show();

            // If video was disliked, remove from dislikes immediately
            if (isDisliked) {
                isDisliked = false;
                dislikeButton.setImageResource(R.drawable.dislike);
            }

            Map<String, Object> videoData = new HashMap<>();
            videoData.put("title", videoTitle);
            videoData.put("videoId", videoId);

            db.collection("users").document(userId)
                    .collection("sessions").document(sessionName)
                    .collection("likeVideos").document(sanitizeTitle(videoTitle))
                    .set(videoData, SetOptions.merge())
                    .addOnFailureListener(e -> {
                        // Rollback UI if operation fails
                        isLiked = false;
                        likeButton.setImageResource(R.drawable.like);
                        Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
                    });

            // If video was disliked, remove from dislikes in database
            if (isDisliked) {
                removeFromDislikes(sessionName);
            }
        }
    }

    private void toggleDislikeStatus(String sessionName) {
        if (isDisliked) {
            // Update UI immediately
            isDisliked = false;
            dislikeButton.setImageResource(R.drawable.dislike);
            Toast.makeText(this, "Removed from dislikes", Toast.LENGTH_SHORT).show();

            // Then update database
            db.collection("users").document(userId)
                    .collection("sessions").document(sessionName)
                    .collection("dislikeVideos").document(sanitizeTitle(videoTitle))
                    .delete()
                    .addOnFailureListener(e -> {
                        // Rollback UI if operation fails
                        isDisliked = true;
                        dislikeButton.setImageResource(R.drawable.disliked);
                        Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Update UI immediately
            isDisliked = true;
            dislikeButton.setImageResource(R.drawable.disliked);
            Toast.makeText(this, "Added to dislikes", Toast.LENGTH_SHORT).show();

            // If video was liked, remove from likes immediately
            if (isLiked) {
                isLiked = false;
                likeButton.setImageResource(R.drawable.like);
            }

            Map<String, Object> videoData = new HashMap<>();
            videoData.put("title", videoTitle);
            videoData.put("videoId", videoId);

            db.collection("users").document(userId)
                    .collection("sessions").document(sessionName)
                    .collection("dislikeVideos").document(sanitizeTitle(videoTitle))
                    .set(videoData, SetOptions.merge())
                    .addOnFailureListener(e -> {
                        // Rollback UI if operation fails
                        isDisliked = false;
                        dislikeButton.setImageResource(R.drawable.dislike);
                        Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
                    });

            // If video was liked, remove from likes in database
            if (isLiked) {
                removeFromLikes(sessionName);
            }
        }
    }

    private void toggleSaveStatus(String sessionName) {
        if (isSaved) {
            // Update UI immediately
            isSaved = false;
            saveButton.setImageResource(R.drawable.save);
            Toast.makeText(this, "Removed from saved videos", Toast.LENGTH_SHORT).show();

            // Then update database
            db.collection("users").document(userId)
                    .collection("sessions").document(sessionName)
                    .collection("saveVideos").document(sanitizeTitle(videoTitle))
                    .delete()
                    .addOnFailureListener(e -> {
                        // Rollback UI if operation fails
                        isSaved = true;
                        saveButton.setImageResource(R.drawable.saved);
                        Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Update UI immediately
            isSaved = true;
            saveButton.setImageResource(R.drawable.saved);
            Toast.makeText(this, "Added to saved videos", Toast.LENGTH_SHORT).show();

            Map<String, Object> videoData = new HashMap<>();
            videoData.put("title", videoTitle);
            videoData.put("videoId", videoId);

            db.collection("users").document(userId)
                    .collection("sessions").document(sessionName)
                    .collection("saveVideos").document(sanitizeTitle(videoTitle))
                    .set(videoData, SetOptions.merge())
                    .addOnFailureListener(e -> {
                        // Rollback UI if operation fails
                        isSaved = false;
                        saveButton.setImageResource(R.drawable.save);
                        Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void removeFromLikes(String sessionName) {
        db.collection("users").document(userId)
                .collection("sessions").document(sessionName)
                .collection("likeVideos").document(sanitizeTitle(videoTitle))
                .delete()
                .addOnSuccessListener(aVoid -> {
                    isLiked = false;
                    likeButton.setImageResource(R.drawable.like);
                });
    }

    private void removeFromDislikes(String sessionName) {
        db.collection("users").document(userId)
                .collection("sessions").document(sessionName)
                .collection("dislikeVideos").document(sanitizeTitle(videoTitle))
                .delete()
                .addOnSuccessListener(aVoid -> {
                    isDisliked = false;
                    dislikeButton.setImageResource(R.drawable.dislike);
                });
    }

}
