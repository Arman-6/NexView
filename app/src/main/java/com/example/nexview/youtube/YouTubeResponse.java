package com.example.nexview.youtube;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class YouTubeResponse {

    @SerializedName("items")
    private List<Item> items;

    @SerializedName("nextPageToken")
    private String nextPageToken;

    // Getter for nextPageToken
    public String getNextPageToken() {
        return nextPageToken;
    }

    // Method to convert API response to VideoModel list
    public List<VideoModel> getVideos() {
        List<VideoModel> videos = new ArrayList<>();
        if (items != null) {
            for (Item item : items) {
                VideoModel video = new VideoModel();
                video.setTitle(item.getSnippet().getTitle());
                video.setDescription(item.getSnippet().getDescription());
                video.setThumbnailUrl(item.getSnippet().getThumbnails().getHigh().getUrl());
                video.setVideoId(item.getId().getVideoId());

                // Add channel title
                video.setChannelTitle(item.getSnippet().getChannelTitle());

                // Add tags if available
                video.setTags(item.getSnippet().getTags());

                // Add duration if contentDetails is available
                if (item.getContentDetails() != null && item.getContentDetails().getDuration() != null) {
                    video.setDuration(item.getContentDetails().getDuration());
                }
                videos.add(video);
            }
        }
        return videos;
    }

    // Inner class for Item
    public static class Item {

        @SerializedName("id")
        private VideoId id;

        @SerializedName("snippet")
        private Snippet snippet;

        @SerializedName("contentDetails")
        private ContentDetails contentDetails; // This is correct, no change needed

        public VideoId getId() {
            return id;
        }

        public Snippet getSnippet() {
            return snippet;
        }

        public ContentDetails getContentDetails() {
            return contentDetails;
        }
    }

    // Inner class for VideoId
    public static class VideoId {

        @SerializedName("videoId")
        private String videoId;

        public String getVideoId() {
            return videoId;
        }
    }

    // Inner class for Snippet
    public static class Snippet {

        @SerializedName("title")
        private String title;

        @SerializedName("description")
        private String description;

        @SerializedName("thumbnails")
        private Thumbnails thumbnails;

        @SerializedName("channelTitle")
        private String channelTitle;

        @SerializedName("tags")
        private List<String> tags;

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public Thumbnails getThumbnails() {
            return thumbnails;
        }

        public String getChannelTitle() {
            return channelTitle;
        }

        public List<String> getTags() {
            return tags;
        }
    }

    // Inner class for Thumbnails
    public static class Thumbnails {

        @SerializedName("high")
        private Thumbnail high;

        public Thumbnail getHigh() {
            return high;
        }
    }

    // Inner class for Thumbnail
    public static class Thumbnail {

        @SerializedName("url")
        private String url;

        public String getUrl() {
            return url;
        }
    }

    // Inner class for ContentDetails
    public static class ContentDetails {

        @SerializedName("duration")
        private String duration;

        public String getDuration() {
            return duration;
        }
    }
}