package com.humanitarian.devui.ui;

import com.humanitarian.devui.model.*;
import com.humanitarian.devui.crawler.DataCrawler;
import com.humanitarian.devui.database.DatabaseManager;
import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Utility class for common crawling operations.
 * Shared logic between CrawlControlPanel and other crawling components.
 */
public class CrawlingUtility {
    private static final Logger LOGGER = Logger.getLogger(CrawlingUtility.class.getName());
    
    /**
     * Add comments to a post using predefined templates
     */
    public static void addCommentsToPost(Post post, int commentLimit) {
        String[] commentTemplates = {
            "The relief distribution was well organized",
            "Not enough resources were provided to affected areas",
            "Great effort from the humanitarian team",
            "Need more medical support in the affected region",
            "Food aid arrived on time",
            "Disappointed with the response time",
            "Excellent coordination with local authorities",
            "More shelter needed for displaced families",
            "Transportation assistance was very helpful",
            "Cash assistance made a big difference"
        };

        int commentCount = Math.min(commentLimit, commentTemplates.length);
        for (int i = 0; i < commentCount; i++) {
            String content = commentTemplates[i];
            Comment comment = new Comment(
                "COMMENT_" + post.getPostId() + "_" + i,
                post.getPostId(),
                content,
                post.getCreatedAt().plusHours(i + 1),
                "User_" + (i + 1)
            );

            // Random sentiment
            Sentiment.SentimentType type = Math.random() > 0.5 ?
                (Math.random() > 0.5 ? Sentiment.SentimentType.POSITIVE : Sentiment.SentimentType.NEGATIVE)
                : Sentiment.SentimentType.NEUTRAL;
            double confidence = 0.7 + Math.random() * 0.3;

            comment.setSentiment(new Sentiment(type, confidence, content));
            comment.setReliefItem(post.getReliefItem());
            post.addComment(comment);
        }
    }
    
    /**
     * Check for duplicate post using database
     * @return true if post is duplicate (already in DB), false if new
     */
    public static boolean isDuplicatePost(String postId) {
        DatabaseManager dbChecker = new DatabaseManager();
        try {
            return dbChecker.isDuplicateLink(postId);
        } catch (Exception e) {
            LOGGER.warning("Error checking duplicate: " + e.getMessage());
            return false;
        } finally {
            try {
                dbChecker.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Process posts after crawling: add comments, check duplicates, assign disaster type
     * @return number of posts that were added (non-duplicates)
     */
    public static int processAndAddPosts(List<Post> posts, SessionDataBuffer buffer, 
                                         List<String> keywords, int commentLimit, 
                                         boolean addCommentsToMocks) {
        int addedCount = 0;
        int duplicateCount = 0;
        
        for (Post post : posts) {
            // Check if already in database
            if (isDuplicatePost(post.getPostId())) {
                duplicateCount++;
                LOGGER.fine("Duplicate post skipped: " + post.getPostId());
                continue;
            }
            
            // Add comments if needed
            if (addCommentsToMocks) {
                addCommentsToPost(post, commentLimit);
            }
            
            // Assign disaster type based on keywords
            if (keywords != null && !keywords.isEmpty()) {
                DisasterType disasterType = findDisasterTypeForPost(post, keywords);
                if (post instanceof YouTubePost) {
                    ((YouTubePost) post).setDisasterType(disasterType);
                }
            }
            
            // Add to buffer
            buffer.addPost(post);
            addedCount++;
        }
        
        LOGGER.info("Processed posts: " + addedCount + " added, " + duplicateCount + " duplicates skipped");
        return addedCount;
    }
    
    /**
     * Find appropriate disaster type for post based on keywords
     */
    public static DisasterType findDisasterTypeForPost(Post post, List<String> keywords) {
        DisasterManager manager = DisasterManager.getInstance();
        
        // Try to find a matching disaster type from the keywords
        for (String keyword : keywords) {
            DisasterType disaster = manager.findDisasterType(keyword);
            if (disaster != null) {
                return disaster;
            }
        }
        
        // Default to "yagi" if no match found
        return manager.getDisasterType("yagi");
    }
    
    /**
     * Validate and clean URLs
     */
    public static List<String> validateAndCleanUrls(String urlText, String platformType) {
        List<String> validUrls = new ArrayList<>();
        
        if (urlText == null || urlText.trim().isEmpty()) {
            return validUrls;
        }
        
        String[] urls = urlText.split("\n");
        
        for (String url : urls) {
            String cleanUrl = url.trim();
            if (cleanUrl.isEmpty()) {
                continue;
            }
            
            // Platform-specific URL validation
            if ("YOUTUBE".equals(platformType)) {
                if (cleanUrl.contains("youtube.com") || cleanUrl.contains("youtu.be")) {
                    validUrls.add(cleanUrl);
                }
            } else {
                // Default: accept any URL with http/https
                if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                    validUrls.add(cleanUrl);
                }
            }
        }
        
        return validUrls;
    }
}
