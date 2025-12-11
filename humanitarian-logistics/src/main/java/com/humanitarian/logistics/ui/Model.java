package com.humanitarian.logistics.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.humanitarian.logistics.analysis.AnalysisModule;
import com.humanitarian.logistics.analysis.SatisfactionAnalysisModule;
import com.humanitarian.logistics.analysis.TimeSeriesSentimentModule;
import com.humanitarian.logistics.database.DataPersistenceManager;
import com.humanitarian.logistics.database.DatabaseManager;
import com.humanitarian.logistics.model.Comment;
import com.humanitarian.logistics.model.Post;
import com.humanitarian.logistics.model.ReliefItem;
import com.humanitarian.logistics.model.Sentiment;
import com.humanitarian.logistics.sentiment.PythonCategoryClassifier;
import com.humanitarian.logistics.sentiment.SentimentAnalyzer;

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
                ReliefItem.Category category = categoryClassifier.classifyText(comment.getContent());
                if (category != null) {
                    comment.setReliefItem(new ReliefItem(category, "ML-classified (Python)", 3));
                }
            }
            if (comment.getSentiment() == null && sentimentAnalyzer != null) {
                Sentiment sentiment = sentimentAnalyzer.analyzeSentiment(comment.getContent());
                comment.setSentiment(sentiment);
            }
            if (comment.getDisasterType() == null || comment.getDisasterType().isEmpty()) {
                String disasterType = post.getDisasterKeyword();
                if (disasterType == null || disasterType.isEmpty()) {
                    disasterType = "N/A";
                }
                comment.setDisasterType(disasterType);
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

    private void loadPersistedData() {
        List<Post> loadedPosts = persistenceManager.loadPosts();
        if (!loadedPosts.isEmpty()) {
            for (Post post : loadedPosts) {
                addPost(post);
            }
            notifyListeners();
        }
    }

    public int analyzeAllPosts() {
        System.out.println("Starting batch analysis of comments from all posts...");
        int analyzedComments = 0;
        int totalComments = 0;

        for (Post post : posts) {
            totalComments += post.getComments().size();
        }

        for (Post post : posts) {
            for (Comment comment : post.getComments()) {
                try {
                    if (comment.getReliefItem() == null) {
                        ReliefItem.Category category = categoryClassifier.classifyText(comment.getContent());
                        if (category != null) {
                            comment.setReliefItem(new ReliefItem(category, "ML-classified (Python)", 3));
                        }
                    }
                    
                    if (sentimentAnalyzer != null) {
                        Sentiment sentiment = sentimentAnalyzer.analyzeSentiment(comment.getContent());
                        comment.setSentiment(sentiment);
                    }
                    
                    try {
                        if (dbManager != null) {
                            dbManager.updateComment(comment);
                        }
                    } catch (Exception dbEx) {
                        System.err.println("Database error: " + dbEx.getMessage());
                    }
                    analyzedComments++;
                } catch (Exception e) {
                    System.err.println("Error analyzing comment " + comment.getCommentId() + ": " + e.getMessage());
                }
            }
        }

        notifyListeners();
        System.out.println("Batch analysis complete! Analyzed " + analyzedComments + "/" + totalComments + " comments");
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

    public void savePersistedData() {
        persistenceManager.savePosts(posts);
    }

    public DataPersistenceManager getPersistenceManager() {
        return persistenceManager;
    }
}
