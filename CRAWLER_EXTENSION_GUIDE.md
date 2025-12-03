# 🚀 Hướng Dẫn Thêm Crawler Mới

**Ngày cập nhật:** December 3, 2025  
**Áp dụng cho:** Humanitarian Logistics & Dev-UI projects

---

## 📌 Tổng Quan

Hệ thống crawler sử dụng **Registry Pattern** để quản lý crawlers một cách dynamic. Nhờ vậy, **thêm crawler mới không cần sửa UI code** - chỉ cần:

1. ✅ Viết class crawler implement `DataCrawler` interface
2. ✅ Register crawler vào `CrawlerManager`
3. ✅ Done! UI tự động cập nhật

---

## 📋 Kiến Trúc Hiện Tại

```
┌─────────────────────────────────────┐
│      CrawlControlPanel (UI)         │
│  (Tự động load crawlers từ registry)│
└────────────────┬────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────┐
│      CrawlerRegistry                │
│  (Quản lý danh sách crawlers)       │
└────────────────┬────────────────────┘
                 │
        ┌────────┼────────┐
        ↓        ↓        ↓
    YouTube  Facebook  Twitter (future)
    Crawler  Crawler   Crawler
```

---

## 🔧 Chi Tiết 3 Bước

### **BƯỚC 1: Tạo Crawler Class**

#### **1.1 Vị trí file**

**Humanitarian Logistics:**
```
humanitarian-logistics/src/main/java/com/humanitarian/logistics/crawler/[YourCrawler].java
```

**Dev-UI:**
```
dev-ui/src/main/java/com/humanitarian/devui/crawler/[YourCrawler].java
```

#### **1.2 Structure cơ bản**

```java
package com.humanitarian.logistics.crawler;

import com.humanitarian.logistics.model.Post;
import java.util.List;
import java.util.ArrayList;

/**
 * Facebook crawler for collecting posts and comments
 * Implements DataCrawler interface for polymorphic usage
 */
public class FacebookCrawler implements DataCrawler {
    
    // ============ STATE ============
    private boolean initialized = false;
    // Add Facebook-specific fields here
    // e.g., private FacebookDriver driver;
    
    // ============ LIFECYCLE ============
    
    /**
     * Initialize crawler (setup browser, API connections, etc)
     * Throws exception if setup fails
     */
    public void initialize() throws Exception {
        // TODO: Setup Facebook connection
        // TODO: Setup authentication
        this.initialized = true;
    }
    
    // ============ CRAWLING OPERATIONS ============
    
    /**
     * Crawl posts based on keywords and hashtags
     * @param keywords List of search terms (e.g., "disaster", "aid")
     * @param hashtags List of hashtags (e.g., "#yagi", "#bualoi")
     * @param limit Maximum number of posts to retrieve
     * @return List of Post objects with comments
     */
    @Override
    public List<Post> crawlPosts(List<String> keywords, List<String> hashtags, int limit) {
        List<Post> posts = new ArrayList<>();
        
        // TODO: Implement Facebook search logic
        // - Search for keywords + hashtags on Facebook
        // - Extract post data (id, content, author, created_at)
        // - Extract comments from each post
        // - Create Post objects and add to list
        
        return posts;
    }
    
    /**
     * Crawl a specific post by URL
     * Optional: only if crawling by URL is supported
     */
    public Post crawlPostByUrl(String url) throws Exception {
        // TODO: Extract post from Facebook URL
        // - Parse URL to get post ID
        // - Fetch post data
        // - Fetch comments
        // - Return Post object
        return null;
    }
    
    // ============ METADATA ============
    
    /**
     * Get crawler identifier name
     * @return Should match the ID registered in CrawlerManager
     */
    @Override
    public String getCrawlerName() {
        return "FacebookCrawler";
    }
    
    /**
     * Check if crawler is ready to use
     * @return true if initialized and ready, false otherwise
     */
    @Override
    public boolean isInitialized() {
        return initialized;
    }
    
    // ============ CLEANUP ============
    
    /**
     * Perform cleanup: close connections, shutdown browser, etc
     */
    @Override
    public void shutdown() {
        // TODO: Close Facebook connections
        // TODO: Cleanup resources
        this.initialized = false;
    }
}
```

#### **1.3 Hàm bắt buộc phải implement**

| Hàm | Mục đích | Return |
|-----|---------|--------|
| `crawlPosts()` | Crawl dữ liệu theo keywords | `List<Post>` |
| `getCrawlerName()` | Lấy tên crawler | `String` |
| `isInitialized()` | Kiểm tra sẵn sàng | `boolean` |
| `shutdown()` | Dọn dẹp resources | `void` |

---

### **BƯỚC 2: Register Crawler Vào CrawlerManager**

#### **2.1 Mở file CrawlerManager.java**

**Humanitarian Logistics:**
```
humanitarian-logistics/src/main/java/com/humanitarian/logistics/crawler/CrawlerManager.java
```

**Dev-UI:**
```
dev-ui/src/main/java/com/humanitarian/devui/crawler/CrawlerManager.java
```

#### **2.2 Tìm method `initializeCrawlers()`**

```java
public static void initializeCrawlers() {
    CrawlerRegistry registry = CrawlerRegistry.getInstance();
    
    // YouTube crawler (hiện tại)
    registry.registerCrawler(...);
    
    // Mock crawler (hiện tại)
    registry.registerCrawler(...);
    
    // ← THÊM CRAWLER MỚI TẠI ĐÂY
}
```

#### **2.3 Thêm registration code**

Thêm vào trước `LOGGER.info()` ở cuối method:

```java
// Register Facebook crawler
registry.registerCrawler(
    new CrawlerRegistry.CrawlerConfig(
        "FACEBOOK",                                    // 1. ID (nội bộ)
        "Facebook",                                    // 2. Tên hiển thị
        "Crawl posts and comments from Facebook",     // 3. Description
        FacebookCrawler::new,                         // 4. Factory method
        true,                                         // 5. Requires initialization?
        true,                                         // 6. Supports keyword search?
        true                                          // 7. Supports URL crawl?
    )
);
```

#### **2.4 Giải thích parameters**

| Parameter | Ý nghĩa | Ví dụ |
|-----------|---------|-------|
| **ID** | Identifier nội bộ, dùng để gọi `createCrawler("FACEBOOK")` | `"FACEBOOK"`, `"TWITTER"` |
| **Display Name** | Tên hiển thị trong dropdown UI | `"Facebook"`, `"Twitter"` |
| **Description** | Mô tả chi tiết, hiển thị khi hover | `"Crawl posts from Facebook"` |
| **Factory** | Reference đến constructor để tạo instance | `FacebookCrawler::new` |
| **Requires Init** | Có cần gọi `initialize()` không? | `true` (Facebook, YouTube cần setup); `false` (Mock) |
| **Keyword Search** | Hỗ trợ crawl theo keywords/hashtags? | `true` (hỗ trợ); `false` (không) |
| **URL Crawl** | Hỗ trợ crawl từ URL trực tiếp? | `true` (Facebook có); `false` (Mock không) |

#### **2.5 Ví dụ đầy đủ - Thêm 2 Crawlers mới**

```java
public static void initializeCrawlers() {
    CrawlerRegistry registry = CrawlerRegistry.getInstance();
    
    // YouTube crawler (hiện tại)
    registry.registerCrawler(
        new CrawlerRegistry.CrawlerConfig(
            "YOUTUBE",
            "YouTube",
            "Crawl videos and comments from YouTube using Selenium",
            YouTubeCrawler::new,
            true,  // Requires browser initialization
            true,  // Supports keyword search
            true   // Supports URL crawl
        )
    );
    
    // Mock data crawler (hiện tại)
    registry.registerCrawler(
        new CrawlerRegistry.CrawlerConfig(
            "MOCK",
            "Sample/Mock Data",
            "Generate sample data for testing (no real crawling)",
            MockDataCrawler::new,
            false, // No initialization needed
            true,  // Supports keyword search
            false  // Does NOT support URL crawl
        )
    );
    
    // ===== THÊM CRAWLERS MỚI DƯỚI ĐÂY =====
    
    // Facebook crawler (MỚI)
    registry.registerCrawler(
        new CrawlerRegistry.CrawlerConfig(
            "FACEBOOK",
            "Facebook",
            "Crawl posts and comments from Facebook",
            FacebookCrawler::new,
            true,  // Requires Facebook API setup
            true,  // Supports keyword search
            true   // Supports URL crawl
        )
    );
    
    // Twitter crawler (MỚI)
    registry.registerCrawler(
        new CrawlerRegistry.CrawlerConfig(
            "TWITTER",
            "Twitter/X",
            "Crawl tweets and replies from Twitter API v2",
            TwitterCrawler::new,
            true,  // Requires Twitter API key
            true,  // Supports keyword search
            true   // Supports URL crawl
        )
    );
    
    LOGGER.info("✓ All crawlers initialized: " + registry.getCrawlerDisplayNames());
}
```

---

### **BƯỚC 3: CrawlControlPanel Tự Động Cập Nhật** ✨

Không cần code thêm! Khi app khởi động:

#### **3.1 Điều gì xảy ra**

1. **CrawlControlPanel constructor gọi `CrawlerManager.initializeCrawlers()`**
   ```java
   if (crawlerRegistry.getCrawlerNames().isEmpty()) {
       CrawlerManager.initializeCrawlers();
   }
   ```

2. **Platform selector dropdown tự động populate**
   ```java
   String[] crawlerNames = crawlerRegistry.getCrawlerDisplayNames()
                                         .toArray(new String[0]);
   // ["YouTube", "Facebook", "Twitter", "Sample/Mock Data"]
   ```

3. **Buttons enable/disable dựa vào capabilities**
   ```java
   if (config.supportsKeywordSearch) {
       crawlButton.setVisible(true);  // Show "Crawl Data" button
   }
   if (config.supportsUrlCrawl) {
       crawlUrlButton.setVisible(true);  // Show "Crawl from URLs" button
   }
   ```

4. **User chọn Facebook → App gọi FacebookCrawler**
   ```java
   crawler = crawlerRegistry.createCrawler("FACEBOOK");
   // → FacebookCrawler::new được gọi
   // → FacebookCrawler instance được tạo
   ```

#### **3.2 UI sẽ tự động có**

- ✅ Dropdown với options: **YouTube | Facebook | Twitter | Sample/Mock Data**
- ✅ Buttons tự động show/hide dựa vào crawler capabilities
- ✅ Khi select Facebook:
  - Nếu `requiresInit = true` → gọi `initialize()`
  - Gọi `crawlPosts()` hoặc `crawlPostByUrl()`
  - Gọi `shutdown()` để cleanup

---

## 🎯 Workflow Đầy Đủ

```
┌─ User khởi động app
│
├─ CrawlControlPanel.__init__()
│  └─ CrawlerManager.initializeCrawlers()
│     └─ Registry.registerCrawler(YouTube, Facebook, Twitter, Mock)
│
├─ UI dropdown được populate:
│  └─ ["YouTube", "Facebook", "Twitter", "Sample/Mock Data"]
│
├─ User chọn "Facebook" từ dropdown
│  └─ updateUIForCrawler()
│     ├─ Config: requiresInit=true, supportsKeyword=true, supportsUrl=true
│     ├─ Show "Crawl Data" button ✓
│     └─ Show "Crawl from URLs" button ✓
│
├─ User clicks "Crawl Data"
│  └─ startCrawling()
│     ├─ crawler = registry.createCrawler("FACEBOOK")
│     │  └─ FacebookCrawler::new được gọi
│     ├─ crawler.initialize()  (vì requiresInit=true)
│     ├─ crawler.crawlPosts(keywords, hashtags, limit)
│     └─ crawler.shutdown()
│
└─ Kết quả hiển thị trên UI
```

---

## 📝 Checklist Khi Thêm Crawler

- [ ] **Tạo file crawler class**
  - [ ] Implement `DataCrawler` interface
  - [ ] Implement `crawlPosts()`
  - [ ] Implement `getCrawlerName()`
  - [ ] Implement `isInitialized()`
  - [ ] Implement `shutdown()`
  - [ ] (Optional) Implement `crawlPostByUrl()`

- [ ] **Register vào CrawlerManager**
  - [ ] Mở `CrawlerManager.java`
  - [ ] Thêm `registerCrawler()` call với đúng config
  - [ ] Set parameters: `requiresInit`, `supportsKeyword`, `supportsUrl`

- [ ] **Test**
  - [ ] Compile project: `mvn clean compile`
  - [ ] Run app
  - [ ] Check dropdown có new crawler option
  - [ ] Test crawl functionality

---

## 🔍 Ví Dụ: Thêm Twitter Crawler

### **File 1: TwitterCrawler.java**

```java
package com.humanitarian.logistics.crawler;

import com.humanitarian.logistics.model.Post;
import java.util.List;
import java.util.ArrayList;

public class TwitterCrawler implements DataCrawler {
    
    private boolean initialized = false;
    private String apiKey;  // Twitter API key
    
    @Override
    public void initialize() throws Exception {
        // Setup Twitter API connection
        if (System.getenv("TWITTER_API_KEY") == null) {
            throw new Exception("TWITTER_API_KEY environment variable not set");
        }
        this.apiKey = System.getenv("TWITTER_API_KEY");
        this.initialized = true;
    }
    
    @Override
    public List<Post> crawlPosts(List<String> keywords, List<String> hashtags, int limit) {
        List<Post> posts = new ArrayList<>();
        // TODO: Call Twitter API v2 to search tweets
        return posts;
    }
    
    @Override
    public String getCrawlerName() {
        return "TwitterCrawler";
    }
    
    @Override
    public boolean isInitialized() {
        return initialized;
    }
    
    @Override
    public void shutdown() {
        this.initialized = false;
    }
}
```

### **File 2: Sửa CrawlerManager.java**

Thêm vào `initializeCrawlers()`:

```java
registry.registerCrawler(
    new CrawlerRegistry.CrawlerConfig(
        "TWITTER",
        "Twitter/X",
        "Crawl tweets using Twitter API v2",
        TwitterCrawler::new,
        true,   // Needs API key setup
        true,   // Can search by keywords
        false   // Cannot crawl by URL (Twitter API limitation)
    )
);
```

### **Kết quả:**
- ✅ Dropdown có option "Twitter/X"
- ✅ "Crawl Data" button visible (keyword search)
- ✅ "Crawl from URLs" button NOT visible (no URL support)
- ✅ App tự động gọi `TwitterCrawler` khi user chọn

---

## ⚠️ Lưu Ý Quan Trọng

### **1. ID phải match với getCrawlerName()**
```java
// CrawlerManager
registry.registerCrawler("FACEBOOK", ...);

// FacebookCrawler
@Override
public String getCrawlerName() {
    return "FacebookCrawler";  // Không cần match chính xác
    // Nhưng nên để rõ ràng để debug dễ
}
```

### **2. Factory method phải đúng**
```java
// ✅ Đúng: FacebookCrawler::new
// ❌ Sai: new FacebookCrawler()  (không phải reference)
registry.registerCrawler(
    new CrawlerRegistry.CrawlerConfig(
        ...,
        FacebookCrawler::new,  // ✅ Correct
        ...
    )
);
```

### **3. Implement đúng interface**
```java
// ✅ Đúng
public class FacebookCrawler implements DataCrawler {
    public List<Post> crawlPosts(List<String> keywords, List<String> hashtags, int limit)
    public String getCrawlerName()
    public boolean isInitialized()
    public void shutdown()
}

// ❌ Sai: bỏ sót method hoặc signature sai
```

### **4. Xử lý exception trong initialize()**
```java
// ✅ Tốt
public void initialize() throws Exception {
    if (condition fails) {
        throw new Exception("Clear error message");
    }
}

// CrawlControlPanel sẽ catch exception và fallback to Mock
```

---

## 🚀 Quick Start Template

Copy-paste template này để bắt đầu:

```java
// FILE: [YourCrawler].java
package com.humanitarian.logistics.crawler;

import com.humanitarian.logistics.model.Post;
import java.util.List;
import java.util.ArrayList;

public class YourCrawler implements DataCrawler {
    
    private boolean initialized = false;
    
    @Override
    public void initialize() throws Exception {
        // TODO: Setup initialization
        this.initialized = true;
    }
    
    @Override
    public List<Post> crawlPosts(List<String> keywords, List<String> hashtags, int limit) {
        List<Post> posts = new ArrayList<>();
        // TODO: Implement crawling logic
        return posts;
    }
    
    @Override
    public String getCrawlerName() {
        return "YourCrawler";
    }
    
    @Override
    public boolean isInitialized() {
        return initialized;
    }
    
    @Override
    public void shutdown() {
        this.initialized = false;
    }
}
```

---

## ❓ FAQ

**Q: Crawler của tôi không cần `initialize()`, làm sao?**
A: Set `requiresInitialization = false` trong CrawlerManager. App sẽ không gọi `initialize()`.

**Q: Làm sao để test crawler?**
A: Tạo unit test hoặc chạy app manual và chọn crawler từ dropdown.

**Q: Có thể có multiple instances của cùng crawler không?**
A: Có, `createCrawler()` luôn tạo instance mới. Mỗi lần crawl là instance mới.

**Q: Exception trong `crawlPosts()` sẽ xảy ra gì?**
A: CrawlControlPanel sẽ catch exception, hiển thị error message, và fallback to Mock data.

**Q: Có thể thay đổi crawler capability sau khi register không?**
A: Không. Capabilities phải fixed khi register. Nếu cần thay đổi, modify config trong `CrawlerManager.initializeCrawlers()`.

---

## 📚 Tham Khảo Thêm

- **DataCrawler Interface**: `src/main/java/.../crawler/DataCrawler.java`
- **CrawlerRegistry**: `src/main/java/.../crawler/CrawlerRegistry.java`
- **CrawlerManager**: `src/main/java/.../crawler/CrawlerManager.java`
- **CrawlControlPanel**: `src/main/java/.../ui/CrawlControlPanel.java`

---

**Happy Crawling!** 🎉
