package com.example.nexview.youtube;

import android.text.Html;
import java.util.List;

public class VideoModel {

    private String title;
    private String description;
    private String thumbnailUrl;
    private String videoId;
    private String duration;
    private String contentDetails;
    private String channelTitle;
    private List<String> tags;
    private String viewCount; // Field for views

    // Constructor with 4 parameters
    public VideoModel(String title, String description, String thumbnailUrl, String videoId) {
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.videoId = videoId;
    }

    // Default constructor
    public VideoModel() {
    }

    // Getter for title with HTML formatting
    public String getTitle() {
        if (title == null) return "";
        return Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY).toString();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // Getter and Setter for description
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Getter and Setter for thumbnail URL
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    // Getter and Setter for video ID
    public String getVideoId() {
        // Fix the getter to return just the ID string
        if (videoId != null && videoId.contains("videoId=")) {
            int startIndex = videoId.indexOf("videoId=") + 8;
            int endIndex = videoId.indexOf("}", startIndex);
            if (endIndex == -1) {
                endIndex = videoId.length();
            }
            return videoId.substring(startIndex, endIndex).trim();
        }
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    // Getter and Setter for duration
    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    // Channel title getter and setter
    public String getChannelTitle() {
        return channelTitle;
    }

    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }

    // Tags getter and setter
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    // Format duration from ISO 8601 format to human-readable format
    public static String formatDuration(String duration) {
        if (duration == null) return "";
        return duration.replace("PT", "").replace("H", "h ").replace("M", "m ").replace("S", "s");
    }

    // View count getter and setter
    public String getViewCount() {
        return viewCount;
    }

    public void setViewCount(String viewCount) {
        this.viewCount = viewCount;
    }

    // Get formatted view count for display
    public String getFormattedViewCount() {
        if (viewCount == null || viewCount.isEmpty()) {
            return "No views";
        }

        try {
            long count = Long.parseLong(viewCount);
            if (count >= 1_000_000_000) {
                return String.format("%.1fB views", count / 1_000_000_000.0);
            } else if (count >= 1_000_000) {
                return String.format("%.1fM views", count / 1_000_000.0);
            } else if (count >= 1_000) {
                return String.format("%.1fK views", count / 1_000.0);
            } else {
                return count + " views";
            }
        } catch (NumberFormatException e) {
            return viewCount + " views";
        }
    }
}