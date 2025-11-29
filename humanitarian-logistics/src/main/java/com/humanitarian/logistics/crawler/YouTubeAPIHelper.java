package com.humanitarian.logistics.crawler;

import com.google.gson.*;
import com.humanitarian.logistics.model.Comment;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * YouTube Official API v3 Helper
 * Provides alternative to HTTP scraping using official YouTube API
 * Logic reused from YoutubeDataCrawler project
 * Uses OkHttp + GSON for JSON parsing (same as YoutubeDataCrawler)
 */
public class YouTubeAPIHelper {
    private final String apiKey;
    private static final String API_BASE = "https://www.googleapis.com/youtube/v3";

    public YouTubeAPIHelper(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Search videos by keyword using official API
     * Logic reused from YoutubeDataCrawler.YouTubeSearch
     */
    public List<String> searchVideos(String query, int maxResults) throws Exception {
        OkHttpClient client = new OkHttpClient();
        List<String> videoIds = new ArrayList<>();

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String url = API_BASE + "/search?" +
                "part=snippet&" +
                "type=video&" +
                "maxResults=" + maxResults + "&" +
                "q=" + encodedQuery + "&" +
                "key=" + apiKey;

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String jsonData = response.body().string();
                JsonObject json = JsonParser.parseString(jsonData).getAsJsonObject();
                JsonArray items = json.getAsJsonArray("items");

                if (items != null) {
                    for (JsonElement item : items) {
                        try {
                            String videoId = item.getAsJsonObject()
                                    .getAsJsonObject("id")
                                    .get("videoId")
                                    .getAsString();
                            videoIds.add(videoId);
                        } catch (Exception e) {
                            // Skip invalid entries
                        }
                    }
                }
            }
        }

        return videoIds;
    }

    /**
     * Get comments for a video using official API
     * Logic reused from YoutubeDataCrawler.YouTubeCommentCrawler
     */
    public List<Comment> getComments(String videoId) throws Exception {
        OkHttpClient client = new OkHttpClient();
        List<Comment> comments = new ArrayList<>();
        String pageToken = "";
        boolean hasNext = true;

        while (hasNext) {
            String url = API_BASE + "/commentThreads?" +
                    "part=snippet&" +
                    "videoId=" + videoId + "&" +
                    "maxResults=100&" +
                    "key=" + apiKey;

            if (!pageToken.isEmpty()) {
                url += "&pageToken=" + pageToken;
            }

            Request request = new Request.Builder().url(url).build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    break;
                }

                String jsonData = response.body().string();
                JsonObject json = JsonParser.parseString(jsonData).getAsJsonObject();
                JsonArray items = json.getAsJsonArray("items");

                if (items != null) {
                    for (JsonElement item : items) {
                        try {
                            JsonObject snippet = item.getAsJsonObject()
                                    .getAsJsonObject("snippet")
                                    .getAsJsonObject("topLevelComment")
                                    .getAsJsonObject("snippet");

                            String author = snippet.get("authorDisplayName").getAsString();
                            String text = snippet.get("textOriginal").getAsString();
                            String publishedAt = snippet.get("publishedAt").getAsString();

                            // Parse ISO date
                            LocalDateTime createdAt = parseISO8601(publishedAt);
                            
                            Comment comment = new Comment(
                                    UUID.randomUUID().toString(),
                                    videoId,
                                    text,
                                    createdAt,
                                    author
                            );
                            comments.add(comment);
                        } catch (Exception e) {
                            // Skip invalid entries
                        }
                    }
                }

                // Check for next page
                if (json.has("nextPageToken")) {
                    pageToken = json.get("nextPageToken").getAsString();
                } else {
                    hasNext = false;
                }
            }
        }

        return comments;
    }

    /**
     * Get video details (title, published date) using official API
     */
    public JsonObject getVideoDetails(String videoId) throws Exception {
        OkHttpClient client = new OkHttpClient();

        String url = API_BASE + "/videos?" +
                "part=snippet,contentDetails&" +
                "id=" + videoId + "&" +
                "key=" + apiKey;

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String jsonData = response.body().string();
                JsonObject json = JsonParser.parseString(jsonData).getAsJsonObject();
                JsonArray items = json.getAsJsonArray("items");

                if (items != null && items.size() > 0) {
                    return items.get(0).getAsJsonObject().getAsJsonObject("snippet");
                }
            }
        }

        return null;
    }

    /**
     * Parse ISO 8601 date format (2024-12-01T10:30:00Z)
     */
    private LocalDateTime parseISO8601(String dateStr) {
        try {
            // Remove 'Z' and replace with +00:00 for parsing
            dateStr = dateStr.replace("Z", "+00:00");
            
            return java.time.OffsetDateTime.parse(dateStr).toLocalDateTime();
        } catch (Exception e) {
            System.err.println("Could not parse date: " + dateStr);
            return LocalDateTime.now();
        }
    }

    /**
     * Check if API key is valid and accessible
     */
    public boolean isAPIKeyValid() {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }

        try {
            OkHttpClient client = new OkHttpClient();
            String url = API_BASE + "/search?part=snippet&q=test&maxResults=1&key=" + apiKey;
            Request request = new Request.Builder().url(url).build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
