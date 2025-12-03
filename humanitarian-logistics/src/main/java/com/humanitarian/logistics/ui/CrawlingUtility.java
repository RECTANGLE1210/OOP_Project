package com.humanitarian.logistics.ui;

import com.humanitarian.logistics.model.*;
import com.humanitarian.logistics.crawler.DataCrawler;
import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Utility class for common crawling operations.
 * Shared logic between CrawlControlPanel and other crawling components.
 * (humanitarian-logistics version - adds posts to Model, not SessionDataBuffer)
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
