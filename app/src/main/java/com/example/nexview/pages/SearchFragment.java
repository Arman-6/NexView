package com.example.nexview.pages;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SearchFragment extends Fragment {

    private EditText searchView;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private VideoAdapter videoAdapter;
    private List<VideoModel> videoList;
    private YouTubeApiService youTubeApiService;
    private String apiKey;
    private String userTopic;
    private String nextPageToken = "";
    private String currentSearchQuery = "";
    private boolean isLoading = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        searchView = view.findViewById(R.id.searchView);
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        videoList = new ArrayList<>();
        videoAdapter = new VideoAdapter(getContext(), videoList, this::openVideoPlayer);
        recyclerView.setAdapter(videoAdapter);

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userTopic = sharedPreferences.getString("selectedTopic", "Technology");
 
        youTubeApiService = createYouTubeApiService();
        apiKey = "AIzaSyC0dLnkIuLRBYe2_s3q_1Yd_gdhQlAL3L0";

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() >= 2) {
                    currentSearchQuery = charSequence.toString();
                    nextPageToken = "";
                    videoList.clear();
                    fetchSearchResults(currentSearchQuery);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (!isLoading && layoutManager != null && layoutManager.findLastVisibleItemPosition() == videoList.size() - 1) {
                    fetchSearchResults(currentSearchQuery);
                }
            }
        });

        return view;
    }

    private void fetchSearchResults(String query) {
        if (isLoading) return;
        progressBar.setVisibility(View.VISIBLE);
        isLoading = true;

        String searchQuery = userTopic + " " + query;

        if (apiKey == null) {
            Toast.makeText(getContext(), "API Quota Exhausted. Try again later.", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            isLoading = false;
            return;
        }

        // Random order type
        String[] orderTypes = {"date", "viewCount", "rating","title","relevance"};
        String randomOrder = orderTypes[new Random().nextInt(orderTypes.length)];

        Call<YouTubeResponse> call = youTubeApiService.getVideos(
                "snippet", 10, searchQuery, "video",
                randomOrder, apiKey, ""
        );

        call.enqueue(new Callback<YouTubeResponse>() {
            @Override
            public void onResponse(@NonNull Call<YouTubeResponse> call, @NonNull Response<YouTubeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<VideoModel> fetchedVideos = response.body().getVideos();

                    if (fetchedVideos != null && !fetchedVideos.isEmpty()) {
                        List<VideoModel> filteredVideos = filterVideosByTopic(fetchedVideos, userTopic, query);
                        List<VideoModel> filteredVideos2 = filterOutShorts(fetchedVideos);

                        if (!filteredVideos.isEmpty() && !filteredVideos2.isEmpty()) {
                            Collections.shuffle(filteredVideos);
                            Collections.shuffle(filteredVideos2);
                            videoList.addAll(filteredVideos);
                            videoList.addAll(filteredVideos2);
                            videoAdapter.notifyDataSetChanged();
                            nextPageToken = response.body().getNextPageToken();
                        } else if (response.body().getNextPageToken() != null && !response.body().getNextPageToken().isEmpty()) {
                            // If all videos were filtered out but there are more pages, try the next page
                            nextPageToken = response.body().getNextPageToken();
                            fetchMoreResults(query);
                        }
                    }
                }else if (response.code() == 403){

                }
                progressBar.setVisibility(View.GONE);
                isLoading = false;
            }

            @Override
            public void onFailure(@NonNull Call<YouTubeResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                isLoading = false;
                Log.e("API_KEY", "Failed to Fetch Videos");
            }
        });
    }
    private void openVideoPlayer(VideoModel video) {
        Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
        intent.putExtra("videoId", video.getVideoId());
        intent.putExtra("videoTitle", video.getTitle());
        startActivity(intent);
    }

    private YouTubeApiService createYouTubeApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.googleapis.com/youtube/v3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(YouTubeApiService.class);
    }

    private void fetchMoreResults(String query) {
        // This method is called when all videos were filtered out but there are more pages
        if (!nextPageToken.isEmpty()) {
            isLoading = true;
            fetchSearchResults(query);
        }
    }

    private List<VideoModel> filterVideosByTopic(List<VideoModel> videos, String topic, String searchQuery) {
        String lowercaseTopic = topic.toLowerCase();
        String lowercaseQuery = searchQuery.toLowerCase();

        List<VideoModel> priorityOne = new ArrayList<>();
        List<VideoModel> priorityTwo = new ArrayList<>();
        List<VideoModel> remainingVideos = new ArrayList<>();

        for (VideoModel video : videos) {
            String title = cleanText(video.getTitle().toLowerCase());
            String description = cleanText(video.getDescription().toLowerCase());
            String channelTitle = cleanText(video.getChannelTitle().toLowerCase());

            boolean containsTopic = title.contains(lowercaseTopic) ||
                    description.contains(lowercaseTopic) ||
                    channelTitle.contains(lowercaseTopic);

            boolean containsQuery = title.contains(lowercaseQuery) ||
                    description.contains(lowercaseQuery) ||
                    channelTitle.contains(lowercaseQuery);

            if (containsTopic && !containsEntertainmentKeywords(title) && !containsEntertainmentKeywords(description)) {
                priorityOne.add(video); // Highest Priority ✅
            } else if (containsQuery && containsTopic && !containsEntertainmentKeywords(title) && !containsEntertainmentKeywords(description)) {
                priorityTwo.add(video); // Second Priority ✅
            } else if (!containsEntertainmentKeywords(title) && !containsEntertainmentKeywords(description)) {
                remainingVideos.add(video); // Last Priority (After Entertainment Filter)
            }
        }

        // Merge all lists together in order
        List<VideoModel> finalList = new ArrayList<>();
        finalList.addAll(priorityOne);
        finalList.addAll(priorityTwo);
        finalList.addAll(remainingVideos);

        return finalList;
    }

    /**
     * Removes emojis and normalizes text.
     */
    private String cleanText(String text) {
        if (text == null) return "";

        // Remove emojis using regex
        text = text.replaceAll("[\\p{So}\\p{Cn}]", "");

        // Normalize text (removes accents and special symbols)
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("[^\\p{ASCII}]", "");

        return text;
    }

    /**
     * Checks if text contains keywords related to entertainment content
     * that we want to filter out.
     */
    private boolean containsEntertainmentKeywords(String text) {
        if (text == null) return false;

        String lowercaseText = text.toLowerCase();
        String[] entertainmentKeywords = {
                "shorts", "#", "#shorts", "gaming", "music video", "official music", "full movie", "trailer", "episode",
                "tv show", "television", "series", "concert", "soundtrack", "vlogs", "vlog",
                "song", "album", "feat.", "ft.", "featuring", "ep", "serial", "show",
                "anupamaa", "kundali bhagya", "yeh rishta kya kehlata hai", "taarak mehta ka ooltah chashmah",
                "bhabi ji ghar par hain", "ghum hai kisi ke pyaar mein", "naagin", "kumkum bhagya", "movie", "bollywood", "hollywood", "pubg", "free fire", "fortnite", "valorant", "gta", "gta v", "call of duty",
                "cod", "fifa", "minecraft", "clash of clans", "clash royale", "bgmi",
                "apex legends", "csgo", "cs go", "candy crush", "among us", "subway surfers",
                "temple run", "league of legends", "dota", "dota 2", "pokemon go",
                "battlegrounds mobile india", "genshin impact", "pubg mobile", "roblox",
                "fortnite battle royale", "rocket league", "god of war", "red dead redemption",
                "pubg lite", "resident evil", "halo", "cyberpunk", "elden ring", "pubg kr",
                "pes", "pes 2021", "mlbb", "mobile legends", "mobile legends bang bang",
                "spiderman", "marvel's spiderman", "the last of us", "fall guys",
                "tomb raider", "watch dogs", "assassin's creed", "ghost of tsushima",
                "death stranding", "battlefield", "battlefield 2042", "rainbow six siege",
                "rainbow six", "far cry", "cyber hunter", "pubg new state", "new state mobile",
                "crossfire", "ragnarok", "resident evil 4", "nfs", "need for speed",
                "grand theft auto", "fifa 23", "nba 2k", "animal crossing", "the witcher",
                "witcher 3", "sea of thieves", "genshin", "nba live", "persona",
                "persona 5", "persona 4", "detroit become human", "skyrim", "valorant mobile"
        };

        for (String keyword : entertainmentKeywords) {
            if (lowercaseText.contains(keyword)) {
                return true;
            }
        }
        return false;
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

}