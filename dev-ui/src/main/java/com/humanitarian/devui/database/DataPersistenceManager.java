package com.humanitarian.devui.database;

import com.humanitarian.devui.model.*;
import java.io.*;
import java.util.*;

/**
 * Manages persistent storage of posts and disaster types for dev-ui.
 * Saves/loads data to/from local cache files.
 */
public class DataPersistenceManager {
    private String postsFile;
    private String disastersFile;
    
    private String getDataDir() {
        // Get current working directory and find the data folder
        String currentDir = new File(".").getAbsolutePath();
        File dataDir;
        
        // If running from dev-ui directory, use dev-ui/data
        if (currentDir.endsWith("dev-ui")) {
            dataDir = new File("data");
        } else {
            // If running from project root, use dev-ui/data
            dataDir = new File("dev-ui/data");
        }
        
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        return dataDir.getAbsolutePath();
    }

    public DataPersistenceManager() {
        String dataDir = getDataDir();
        this.postsFile = dataDir + "/posts.dat";
        this.disastersFile = dataDir + "/disasters.dat";
        // Data directory is already created in getDataDir() static method
    }

    /**
     * Save posts to persistent storage
     */
    public void savePosts(List<Post> posts) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(postsFile))) {
            oos.writeObject(new ArrayList<>(posts));
            System.out.println("✓ Posts saved: " + posts.size() + " items");
        } catch (IOException e) {
            System.err.println("Error saving posts: " + e.getMessage());
        }
    }

    /**
     * Load posts from persistent storage
     */
    @SuppressWarnings("unchecked")
    public List<Post> loadPosts() {
        File file = new File(postsFile);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(postsFile))) {
            List<Post> posts = (List<Post>) ois.readObject();
            System.out.println("✓ Posts loaded: " + posts.size() + " items");
            return posts;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading posts: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Save disaster types to persistent storage
     */
    public void saveDisasters(DisasterManager manager) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(disastersFile))) {
            // Save all non-default disaster types
            Set<String> defaultDisasters = new HashSet<>(Arrays.asList(
                "yagi", "matmo", "bualo", "koto", "fung-wong"
            ));
            
            Map<String, DisasterType> customDisasters = new HashMap<>();
            for (String name : manager.getAllDisasterNames()) {
                if (!defaultDisasters.contains(name.toLowerCase())) {
                    customDisasters.put(name, manager.getDisasterType(name));
                }
            }
            
            oos.writeObject(customDisasters);
            System.out.println("✓ Disaster types saved: " + customDisasters.size() + " custom types");
        } catch (IOException e) {
            System.err.println("Error saving disasters: " + e.getMessage());
        }
    }

    /**
     * Load custom disaster types from persistent storage
     */
    @SuppressWarnings("unchecked")
    public void loadDisasters(DisasterManager manager) {
        File file = new File(disastersFile);
        if (!file.exists()) {
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(disastersFile))) {
            Map<String, DisasterType> customDisasters = (Map<String, DisasterType>) ois.readObject();
            for (DisasterType disaster : customDisasters.values()) {
                manager.addDisasterType(disaster);
            }
            System.out.println("✓ Custom disaster types loaded: " + customDisasters.size() + " types");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading disasters: " + e.getMessage());
        }
    }

    /**
     * Clear all persistent data
     */
    public void clearAllData() {
        File postsFileObj = new File(postsFile);
        File disastersFileObj = new File(disastersFile);
        
        if (postsFileObj.exists()) {
            postsFileObj.delete();
        }
        if (disastersFileObj.exists()) {
            disastersFileObj.delete();
        }
        
        System.out.println("✓ All persistent data cleared");
    }

    /**
     * Check if saved data exists
     */
    public boolean hasSavedData() {
        return new File(postsFile).exists();
    }

    /**
     * Get data directory path
     */
    public String getDataDirectory() {
        return getDataDir();
    }
}
