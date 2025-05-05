package com.example.nexview.youtube;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface YouTubeApiService {
    @GET("search")
    Call<YouTubeResponse> getVideos(
            @Query("part") String part,
            @Query("maxResults") int maxResults,
            @Query("q") String query,
            @Query("type") String type,
            @Query("order") String order,
            @Query("key") String apiKey,
            @Query("pageToken") String pageToken
            );

    // Fetch video details
    @GET("videos")
    Call<YouTubeResponse> getVideoDetails(
            @Query("part") String part,      // Should include "contentDetails"
            @Query("id") String videoId,     // Pass the Video ID
            @Query("key") String apiKey      // Your API Key
    );

}
