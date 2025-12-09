package com.humanitarian.logistics.ui;

import com.humanitarian.logistics.model.*;
import com.humanitarian.logistics.sentiment.SentimentAnalyzer;
import com.humanitarian.logistics.sentiment.PythonCategoryClassifier;
import com.humanitarian.logistics.database.DatabaseManager;
import com.humanitarian.logistics.database.DataPersistenceManager;
import com.humanitarian.logistics.analysis.*;

import java.util.*;

public class Model {
    private List<Post> posts;
    private SentimentAnalyzer sentimentAnalyzer;
    private PythonCategoryClassifier categoryClassifier;
    private DatabaseManager dbManager;
    private DataPersistenceManager persistenceManager;
    private Map<String, AnalysisModule> analysisModules;
    private List<ModelListener> listeners;

    public Model() {
        this.posts = new ArrayList<>();
        this.listeners = new ArrayList<>();
        this.analysisModules = new LinkedHashMap<>();
        this.categoryClassifier = new PythonCategoryClassifier();
        this.dbManager = new DatabaseManager();
        this.persistenceManager = new DataPersistenceManager();

        registerAnalysisModules();
        
        loadPersistedData();
    }

    private void registerAnalysisModules() {
        analysisModules.put("satisfaction", new SatisfactionAnalysisModule());
        analysisModules.put("time_series", new TimeSeriesSentimentModule());
    }

    public void setSentimentAnalyzer(SentimentAnalyzer analyzer) {
        if (this.sentimentAnalyzer != null) {
            this.sentimentAnalyzer.shutdown();
        }
        this.sentimentAnalyzer = analyzer;
        this.sentimentAnalyzer.initialize();
        notifyListeners();
    }

    public List<Post> getPosts() {
        return new ArrayList<>(posts);
    }

    public void clearPosts() {
        posts.clear();
        notifyListeners();
    }

    public void addPost(Post post) {

        if (post.getReliefItem() == null) {
            categoryClassifier.classifyPost(post);
        }

        if (post.getSentiment() == null && sentimentAnalyzer != null) {
            Sentiment sentiment = sentimentAnalyzer.analyzeSentiment(post.getContent());
            post.setSentiment(sentiment);
        }

        for (Comment comment : post.getComments()) {
            if (comment.getReliefItem() == null) {
                // Classify comment category directly using Python API
                ReliefItem.Category category = categoryClassifier.classifyText(comment.getContent());
                if (category != null) {
                    comment.setReliefItem(new ReliefItem(category, "ML-classified (Python)", 3));
                    System.out.println("  ✓ Comment category classified: " + category.getDisplayName());
                }
            }
            if (comment.getSentiment() == null && sentimentAnalyzer != null) {
                Sentiment sentiment = sentimentAnalyzer.analyzeSentiment(comment.getContent());
                comment.setSentiment(sentiment);
            }
            // Save comment to database after analysis
            try {
                if (dbManager != null) {
                    System.out.println("  DEBUG: Saving comment " + comment.getCommentId() + 
                        " | Sentiment: " + (comment.getSentiment() != null ? comment.getSentiment().getType() : "null") +
                        " | Category: " + (comment.getReliefItem() != null ? comment.getReliefItem().getCategory() : "null"));
                    dbManager.updateComment(comment);
                } else {
                    System.err.println("  Warning: dbManager is null, cannot save comment");
                }
            } catch (Exception e) {
                System.err.println("Error saving comment to database: " + e.getMessage());
                e.printStackTrace();
            }
        }

        this.posts.add(post);
        try {
            dbManager.savePost(post);
        } catch (Exception e) {
            System.err.println("Error saving post: " + e.getMessage());
        }
        notifyListeners();
    }

    public void addPosts(List<Post> newPosts) {
        for (Post post : newPosts) {
            addPost(post);
        }
    }

    public void updateComment(Comment updatedComment) {
        for (Post post : posts) {
            for (Comment comment : post.getComments()) {
                if (comment.getCommentId().equals(updatedComment.getCommentId())) {
                    post.updateComment(updatedComment);
                    notifyListeners();
                    return;
                }
            }
        }
    }

    public void removeComment(String commentId) {
        for (Post post : posts) {
            for (Comment comment : post.getComments()) {
                if (comment.getCommentId().equals(commentId)) {
                    post.removeComment(commentId);
                    notifyListeners();
                    return;
                }
            }
        }
    }

    public Map<String, Object> performAnalysis(String moduleName) {
        AnalysisModule module = analysisModules.get(moduleName);
        if (module == null) {
            return Collections.emptyMap();
        }
        return module.analyze(posts);
    }

    public Map<String, AnalysisModule> getAnalysisModules() {
        return new LinkedHashMap<>(analysisModules);
    }

    public void addModelListener(ModelListener listener) {
        listeners.add(listener);
    }

    public void removeModelListener(ModelListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (ModelListener listener : listeners) {
            listener.modelChanged();
        }
    }

    private static class PostAdapter extends Post {
        PostAdapter(Comment comment) {
            super(comment.getCommentId(), comment.getContent(), comment.getCreatedAt(),
                    comment.getAuthor(), "ADAPTER");
        }
    }

    private void loadPersistedData() {
        List<Post> loadedPosts = persistenceManager.loadPosts();
        if (!loadedPosts.isEmpty()) {

            for (Post post : loadedPosts) {
                addPost(post);
            }
            notifyListeners();
            System.out.println("✓ Persisted data loaded: " + loadedPosts.size() + " posts");
        }
    }

    public void savePersistedData() {
        persistenceManager.savePosts(posts);
    }

    public void clearPersistedData() {
        persistenceManager.clearAllData();
        posts.clear();
        notifyListeners();
    }

    public DataPersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    public int analyzeAllPosts() {
        System.out.println("Starting batch analysis of comments from all posts...");
        System.out.println("✓ Category Classification: Keyword-based (Instant Vietnamese)");
        System.out.println("✓ Sentiment Analysis: xlm-roberta-large-xnli (Vietnamese + 100+ languages)");
        int analyzedComments = 0;
        int totalComments = 0;

        // Count total comments
        for (Post post : posts) {
            totalComments += post.getComments().size();
        }

        // Analyze only comments, not posts
        for (Post post : posts) {
            for (Comment comment : post.getComments()) {
                try {
                    // Classify comment category directly using Python API
                    if (comment.getReliefItem() == null) {
                        ReliefItem.Category category = categoryClassifier.classifyText(comment.getContent());
                        if (category != null) {
                            comment.setReliefItem(new ReliefItem(category, "ML-classified (Python)", 3));
                            System.out.println("  ✓ Category: " + category.getDisplayName());
                        }
                    }
                    
                    // Analyze comment sentiment
                    if (sentimentAnalyzer != null) {
                        Sentiment sentiment = sentimentAnalyzer.analyzeSentiment(comment.getContent());
                        comment.setSentiment(sentiment);
                    }
                    
                    // Save comment to database
                    try {
                        if (dbManager != null) {
                            System.out.println("  DEBUG: Saving comment " + comment.getCommentId() + 
                                " | Sentiment: " + (comment.getSentiment() != null ? comment.getSentiment().getType() : "null") +
                                " | Category: " + (comment.getReliefItem() != null ? comment.getReliefItem().getCategory() : "null"));
                            dbManager.updateComment(comment);
                            System.out.println("  ✓ Comment saved to database");
                        } else {
                            System.err.println("  ✗ ERROR: dbManager is null!");
                        }
                    } catch (Exception dbEx) {
                        System.err.println("  ✗ Database error: " + dbEx.getMessage());
                        dbEx.printStackTrace();
                    }
                    analyzedComments++;

                    System.out.println("✓ Analyzed comment " + analyzedComments + "/" + totalComments + 
                        " (ID: " + comment.getCommentId() + ")");
                } catch (Exception e) {
                    System.err.println("✗ Error analyzing comment " + comment.getCommentId() + ": " + e.getMessage());
                }
            }
        }

        notifyListeners();
        System.out.println("✓ Batch analysis complete! Analyzed " + analyzedComments + "/" + totalComments + " comments");
        return analyzedComments;
    }

    public void resetDatabaseConnection() {
        if (dbManager != null) {
            try {

                dbManager.reset();
            } catch (Exception e) {
                System.err.println("Error resetting dbManager: " + e.getMessage());
            }
        }
    }
}
