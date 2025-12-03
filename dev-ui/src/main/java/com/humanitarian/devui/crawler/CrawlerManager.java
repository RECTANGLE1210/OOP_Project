package com.humanitarian.devui.crawler;

import java.util.logging.Logger;

/**
 * Initializes and manages available crawlers for the application.
 * This class should be called once at application startup to register all available crawlers.
 * 
 * To add a new crawler:
 * 1. Create a class that implements DataCrawler
 * 2. Add a registration call in initializeCrawlers()
 * No UI changes needed!
 */
public class CrawlerManager {
    private static final Logger LOGGER = Logger.getLogger(CrawlerManager.class.getName());
    
    /**
     * Initialize and register all available crawlers
     * Call this once at application startup
     */
    public static void initializeCrawlers() {
        CrawlerRegistry registry = CrawlerRegistry.getInstance();
        
        // Register YouTube crawler
        registry.registerCrawler(
            new CrawlerRegistry.CrawlerConfig(
                "YOUTUBE",
                "YouTube",
                "Crawl videos and comments from YouTube using Selenium",
                YouTubeCrawler::new,
                true,  // Requires initialization (browser setup)
                true,  // Supports keyword search
                true   // Supports URL crawl
            )
        );
        
        // Register Mock data crawler (fallback/testing)
        registry.registerCrawler(
            new CrawlerRegistry.CrawlerConfig(
                "MOCK",
                "Sample/Mock Data",
                "Generate sample data for testing (no real crawling)",
                MockDataCrawler::new,
                false, // No initialization needed
                true,  // Supports keyword search
                false  // Does not support URL crawl
            )
        );
        
        // Add more crawlers here in the future:
        // registry.registerCrawler(new CrawlerRegistry.CrawlerConfig(
        //     "FACEBOOK",
        //     "Facebook",
        //     "Crawl posts and comments from Facebook",
        //     FacebookCrawler::new,
        //     true,  // Requires setup
        //     true,  // Keyword search
        //     true   // URL crawl
        // ));
        
        LOGGER.info("✓ All crawlers initialized: " + registry.getCrawlerDisplayNames());
    }
}
