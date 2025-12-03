package com.humanitarian.devui.crawler;

import java.util.*;
import java.util.logging.Logger;

/**
 * Registry for managing available crawlers dynamically.
 * Allows UI to auto-detect and register new crawlers without code changes.
 * 
 * Usage:
 * - Register crawlers at application startup
 * - Query available crawlers by name or type
 * - Create crawler instances on demand
 */
public class CrawlerRegistry {
    private static final Logger LOGGER = Logger.getLogger(CrawlerRegistry.class.getName());
    private static final CrawlerRegistry INSTANCE = new CrawlerRegistry();
    
    private final Map<String, CrawlerFactory> crawlers = new LinkedHashMap<>();
    
    /**
     * Functional interface for creating crawler instances
     */
    @FunctionalInterface
    public interface CrawlerFactory {
        DataCrawler create();
    }
    
    /**
     * Configuration for a registered crawler
     */
    public static class CrawlerConfig {
        public final String name;
        public final String displayName;
        public final String description;
        public final CrawlerFactory factory;
        public final boolean requiresInitialization;
        public final boolean supportsKeywordSearch;
        public final boolean supportsUrlCrawl;
        
        public CrawlerConfig(String name, String displayName, String description, 
                            CrawlerFactory factory, boolean requiresInit, 
                            boolean supportsKeywords, boolean supportsUrl) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.factory = factory;
            this.requiresInitialization = requiresInit;
            this.supportsKeywordSearch = supportsKeywords;
            this.supportsUrlCrawl = supportsUrl;
        }
    }
    
    private final Map<String, CrawlerConfig> crawlerConfigs = new LinkedHashMap<>();
    
    private CrawlerRegistry() {}
    
    /**
     * Get singleton instance
     */
    public static CrawlerRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * Register a new crawler with configuration
     */
    public void registerCrawler(CrawlerConfig config) {
        crawlers.put(config.name, config.factory);
        crawlerConfigs.put(config.name, config);
        LOGGER.info("✓ Registered crawler: " + config.displayName);
    }
    
    /**
     * Register a simple crawler (defaults to both keyword and URL support, no init required)
     */
    public void registerCrawler(String name, String displayName, String description, 
                                CrawlerFactory factory) {
        registerCrawler(new CrawlerConfig(name, displayName, description, factory, false, true, true));
    }
    
    /**
     * Get all registered crawler names
     */
    public List<String> getCrawlerNames() {
        return new ArrayList<>(crawlers.keySet());
    }
    
    /**
     * Get all registered crawler display names
     */
    public List<String> getCrawlerDisplayNames() {
        return crawlerConfigs.values().stream()
            .map(c -> c.displayName)
            .toList();
    }
    
    /**
     * Get crawler configuration by name
     */
    public CrawlerConfig getConfig(String crawlerName) {
        return crawlerConfigs.get(crawlerName);
    }
    
    /**
     * Create a new crawler instance by name
     */
    public DataCrawler createCrawler(String crawlerName) {
        CrawlerFactory factory = crawlers.get(crawlerName);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown crawler: " + crawlerName);
        }
        return factory.create();
    }
    
    /**
     * Check if crawler supports keyword-based search
     */
    public boolean supportsKeywordSearch(String crawlerName) {
        CrawlerConfig config = crawlerConfigs.get(crawlerName);
        return config != null && config.supportsKeywordSearch;
    }
    
    /**
     * Check if crawler supports URL-based crawling
     */
    public boolean supportsUrlCrawl(String crawlerName) {
        CrawlerConfig config = crawlerConfigs.get(crawlerName);
        return config != null && config.supportsUrlCrawl;
    }
    
    /**
     * Check if crawler requires initialization (e.g., browser setup)
     */
    public boolean requiresInitialization(String crawlerName) {
        CrawlerConfig config = crawlerConfigs.get(crawlerName);
        return config != null && config.requiresInitialization;
    }
    
    /**
     * Get description of a crawler
     */
    public String getDescription(String crawlerName) {
        CrawlerConfig config = crawlerConfigs.get(crawlerName);
        return config != null ? config.description : "No description available";
    }
}
