# Danh Sách Công Nghệ & Thuật Toán - Humanitarian Logistics Analysis System

## 1. CÔNG NGHỆ CHÍNH

### 1.1 Backend - Java
- **Phiên Bản**: Java 11 (Compiler & Target)
- **Build Tool**: Maven 3.9.11
- **Architecture Pattern**: MVC (Model-View-Controller) + Layered Architecture

### 1.2 Frontend - Java Swing
- **UI Framework**: Java Swing (javax.swing)
- **Charting Library**: JFreeChart 1.5.3 (Biểu đồ trực quan)
- **Visualizations**: Bar charts, Pie charts, Time series charts, Interactive charts

### 1.3 Database
- **Type**: SQLite (Relational Database)
- **Driver**: sqlite-jdbc 3.44.0.0
- **Location**: `/humanitarian-logistics/data/humanitarian_logistics_user.db` (User data)
- **Location**: `/humanitarian-logistics/data/humanitarian_logistics_curated.db` (Curated dataset - 31 posts)
- **Schema**: 
  - **posts table** (9 columns): id, author_id, title, content, created_at, sentiment_type, sentiment_confidence, disaster_type, relief_item
  - **comments table**: id, post_id, author_id, content, created_at, sentiment_type, sentiment_confidence, disaster_type, relief_item

### 1.4 Machine Learning - Python
- **Python Version**: 3.12.7
- **Framework**: Flask 2.3.0+ (REST API)
- **ML Framework**: Hugging Face Transformers 4.30.0+
- **Deep Learning**: PyTorch 2.0.0+ (GPU-accelerated with CUDA support)
- **Numerical Computing**: NumPy 1.24.0+
- **ML Library**: scikit-learn 1.3.0+ (Utility functions)

#### ML Models Used:
1. **Sentiment Analysis Model**: `xlm-roberta-large-xnli`
   - Hỗ trợ: Vietnamese (Tiếng Việt) + English + 100+ ngôn ngữ khác
   - Task: Zero-shot sentiment classification (POSITIVE, NEGATIVE, NEUTRAL)
   - Output: Sentiment label + Confidence score (0-1)
   - Model Size: ~2GB (cached locally after first download)

2. **Category Classification Model**: `facebook/bart-large-mnli`
   - Hỗ trợ: Vietnamese + English
   - Task: Zero-shot multi-class classification
   - Categories: FOOD, WATER, SHELTER, MEDICAL, CASH, TRANSPORTATION
   - Output: Relief Item Category + Confidence score

### 1.5 Web Scraping & API Integration
- **Selenium**: 4.15.0 (Web scraping)
  - Hỗ trợ: YouTube, Facebook, Twitter (X) crawling
  - WebDriver: Chrome/Firefox automation
  
- **HTTP Libraries**:
  - Apache HttpClient: 4.5.14 (Java HTTP requests)
  - OkHttp3: 4.11.0 (Alternative HTTP client)
  - cURL (Shell scripts for API health checks)

- **YouTube API Integration**: 
  - YouTubeAPIHelper class
  - YouTubeCrawler (DataCrawler implementation)
  - Comment & Video metadata extraction

### 1.6 JSON Processing
- **org.json**: 20231013 (JSON parsing & generation)
- **Gson**: 2.10.1 (JSON serialization/deserialization)

### 1.7 Logging & Utilities
- **SLF4J** (Simple Logging Facade): 2.0.9
  - slf4j-api (Logging interface)
  - slf4j-simple (Simple implementation)
- **Java Logging**: java.util.logging.Logger

### 1.8 Testing Framework
- **JUnit**: 4.13.2 (Unit testing)

### 1.9 Build Plugins
- **maven-compiler-plugin**: 3.11.0 (Compilation)
- **maven-jar-plugin**: 3.3.0 (JAR packaging)
- **maven-assembly-plugin**: 3.6.0 (JAR with dependencies)

---

## 2. THUẬT TOÁN & PHƯƠNG PHÁP PHÂN TÍCH

### 2.1 Sentiment Analysis (Phân Tích Cảm Xúc)

#### A. Python-based Sentiment Analysis (Chính)
- **Model**: xlm-roberta-large-xnli
- **Method**: Zero-shot classification using pretrained transformer model
- **Flow**:
  1. Text preprocessing (lowercasing, whitespace trimming)
  2. Tokenization using transformer tokenizer
  3. Forward pass through xlm-roberta model
  4. Softmax probability over classes: [POSITIVE, NEGATIVE, NEUTRAL]
  5. Return top class + confidence score

#### B. Fallback Sentiment Analysis (Java-based)
- **Class**: SimpleSentimentAnalyzer
- **Method**: Keyword-based heuristic matching
- **Algorithm**:
  1. Convert text to lowercase
  2. Count positive keywords: good, great, excellent, happy, love, thank, appreciate, support, help, aid, relief, better, improved, success, wonderful, fantastic, amazing
  3. Count negative keywords: bad, poor, terrible, sad, hate, angry, upset, frustrated, struggle, difficult, problem, issue, lack, missing, needed, fail, failure, disaster, crisis, emergency
  4. Calculate sentiment type:
     - If positive_count > negative_count → POSITIVE
     - If negative_count > positive_count → NEGATIVE
     - Otherwise → NEUTRAL
  5. Confidence = max(positive_count, negative_count) / total_words (capped at 0.0-1.0)

#### C. Enhanced Sentiment Analysis (Vietnamese-specific)
- **Class**: EnhancedSentimentAnalyzer
- **Method**: Multi-dimensional sentiment scoring with Vietnamese keywords
- **Features**:
  - Vietnamese positive keywords: tốt, xuất sắc, tuyệt vời, vui, yêu, cảm ơn, hỗ trợ, giúp đỡ, cứu trợ, tốt hơn, thành công, tuyệt diệu, tuyệt vời, tuyệt vời
  - Vietnamese negative keywords: tệ, chất lượng thấp, khủng khiếp, buồn, ghét, tức giận, bất bình, tuyệt vọng, đấu tranh, khó khăn, vấn đề, vấn đề, thiếu, mất tích, cần thiết, thất bại, thất bại, thảm họa, khủng hoảng, khẩn cấp
  - Multi-word phrase detection
  - Sentiment score calculation

### 2.2 Category Classification (Phân Loại Relief Item)

#### A. Python-based Category Classification
- **Model**: facebook/bart-large-mnli
- **Method**: Zero-shot multi-class classification
- **Categories**: 
  - FOOD (thực phẩm)
  - WATER (nước uống)
  - SHELTER (chỗ ở)
  - MEDICAL (y tế)
  - CASH (tiền mặt / hỗ trợ tài chính)
  - TRANSPORTATION (vận chuyển / giao thông)
- **Implementation**:
  1. Define category labels with keywords
  2. Zero-shot classification: predict which category best matches text
  3. Output: Category + Confidence score + Keywords matched

#### B. Keyword-based Category Classification
- **Class**: ReliefItemClassifier
- **Method**: Simple keyword matching with category dictionary
- **Process**:
  1. For each category, define relevant keywords
  2. Count keyword matches in text
  3. Return highest scoring category
  4. Default: Unknown if no strong match

### 2.3 Analysis Modules (Module Phân Tích)

#### Problem 1: Satisfaction Analysis Per Category
- **Class**: SatisfactionAnalysisModule
- **Algorithm**:
  1. Group sentiments by relief category
  2. For each category, calculate:
     - **Count Statistics**:
       - Total mentions
       - Positive count, Negative count, Neutral count
       - Positive percentage, Negative percentage, Neutral percentage
     - **Confidence Metrics**:
       - Average confidence score across all sentiments
     - **Satisfaction Score** = (positive_count - negative_count) / total_sentiments
       - Range: -1.0 (most negative) to +1.0 (most positive)
     - **Effectiveness Assessment**: 
       - Excellent: satisfaction_score ≥ 0.5
       - Good: satisfaction_score ≥ 0.2
       - Fair: satisfaction_score ≥ -0.2
       - Poor: satisfaction_score < -0.2
  3. Generate insights:
     - Detailed category breakdown
     - Resource allocation recommendations
     - Summary statistics

#### Problem 2: Time Series Sentiment Analysis
- **Class**: TimeSeriesSentimentModule
- **Algorithm**:
  1. **Time Bucketing** (6-hour intervals):
     - Group sentiments by category AND time bucket
     - Each bucket = 6-hour period
     - Use TreeMap for chronological ordering
  2. **Time Series Aggregation**:
     - For each time bucket, calculate:
       - Positive, Negative, Neutral counts
       - Average confidence
       - Sentiment trend
       - Trend direction: Improving (↑), Declining (↓), Stable (→)
  3. **Trend Detection**:
     - Compare consecutive time periods
     - Calculate sentiment velocity (rate of change)
     - Identify turning points
  4. **Sector Effectiveness**:
     - Evaluate relief effectiveness over time
     - Status: Stabilizing, Improving, Declining, Critical
  5. **Output**:
     - Time-ordered sentiment progression per category
     - Trend analysis
     - Effectiveness assessment by sector
     - Summary with key time periods

### 2.4 Data Processing & Filtering

#### Text Preprocessing
- **Class**: BasicTextPreprocessor
- **Operations**:
  - Lowercasing
  - Whitespace normalization
  - Special character handling
  - Vietnamese diacritics preservation

#### Data Aggregation
- **Batch Processing**:
  - Analyze all posts simultaneously
  - Process comments associated with each post
  - Aggregate results by category AND sentiment type
- **Streaming Operations**: Java Streams API for functional data transformation

### 2.5 Design Patterns

#### A. Strategy Pattern
- **SentimentAnalyzer** interface:
  - PythonSentimentAnalyzer (Production - uses Python API)
  - SimpleSentimentAnalyzer (Fallback - keyword-based)
  - EnhancedSentimentAnalyzer (Vietnamese-specific)
  - Allows runtime switching between implementations
  
#### B. Factory Pattern
- **CrawlerRegistry** with CrawlerFactory
- **DataCrawler** interface:
  - MockDataCrawler (Testing - 31 curated posts)
  - YouTubeCrawler (Production - YouTube scraping)
  - Extensible for Twitter, Facebook crawlers

#### C. Observer Pattern (MVC Architecture)
- **Model**: Core data & business logic
- **View**: JPanel-based UI components
- **Controller**: Listener-based event handling
- **ModelListener** interface: UI components listen to data changes

#### D. Singleton Pattern (Implicit)
- Database connections
- API endpoints
- Configuration management

---

## 3. DATA FLOW ARCHITECTURE

### 3.1 Data Collection Pipeline
```
YouTube/Facebook/Twitter (Web)
         ↓
    Selenium Crawler
         ↓
Post/Comment Extraction
         ↓
SQLite Database Storage
```

### 3.2 Sentiment Analysis Pipeline
```
Raw Text (Post/Comment)
         ↓
Python Flask API (localhost:5001)
         ↓
xlm-roberta-large-xnli Model
         ↓
Sentiment Classification (POSITIVE/NEGATIVE/NEUTRAL)
         ↓
Confidence Score + Category Classification
         ↓
Return JSON Response
         ↓
Java Application Storage
```

### 3.3 Analysis Pipeline
```
Posts + Comments (SQLite)
         ↓
Filter by Category & Sentiment
         ↓
Module 1: Satisfaction Analysis
  - Calculate sentiment percentages
  - Category effectiveness scores
  - Resource recommendations
         ↓
Module 2: Time Series Analysis
  - Group by time buckets (6 hours)
  - Trend detection
  - Sector effectiveness tracking
         ↓
UI Visualization (JFreeChart)
  - Bar charts (Problem 1)
  - Time series line charts (Problem 2)
  - Pie charts (Sentiment distribution)
  - Interactive charts (MouseListener)
```

---

## 4. ADVANCED FEATURES & OPTIMIZATIONS

### 4.1 Model Caching
- ML models downloaded once on first install
- Cached locally (~2GB for both models)
- Subsequent API calls use cached models (30 seconds startup)

### 4.2 Batch Processing
- `/analyze_batch` endpoint in Flask API
- Process multiple texts in single API call
- Reduces network overhead
- Efficient transformer batch inference

### 4.3 GPU Acceleration
- PyTorch with CUDA support detection
- Automatic GPU usage if available
- CPU fallback if GPU not available
- Model device management in sentiment_api.py

### 4.4 Interactive Visualizations
- **ChartMouseListener** for chart interaction
- Click to drill-down into data
- Tooltip display on hover
- Real-time chart updates

### 4.5 Database Optimization
- SQLite connection pooling
- Prepared statements for SQL injection prevention
- Indexed queries for fast retrieval
- Transaction management

### 4.6 Error Handling & Fallback
- API unavailable → SimpleSentimentAnalyzer fallback
- Category classification failure → Keyword-based classifier
- Network timeout handling with retry logic
- Graceful degradation

---

## 5. TECHNOLOGY STACK SUMMARY TABLE

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Frontend** | Java Swing | Built-in | Desktop UI |
| **Frontend** | JFreeChart | 1.5.3 | Data Visualization |
| **Backend** | Java | 11 | Core Application |
| **Build** | Maven | 3.11.0 | Project Build |
| **Database** | SQLite | 3.44.0.0 | Data Storage |
| **ML/NLP** | Transformers | 4.30.0+ | Sentiment Analysis |
| **ML/DL** | PyTorch | 2.0.0+ | Deep Learning |
| **ML/Utils** | scikit-learn | 1.3.0+ | ML Utilities |
| **Web Framework** | Flask | 2.3.0+ | Python REST API |
| **Web Scraping** | Selenium | 4.15.0 | Web Crawling |
| **HTTP** | HttpClient | 4.5.14 | API Requests |
| **JSON** | org.json | 20231013 | JSON Processing |
| **Logging** | SLF4J | 2.0.9 | Logging |
| **Testing** | JUnit | 4.13.2 | Unit Tests |

---

## 6. KEY ALGORITHMS AT A GLANCE

| Algorithm | Problem | Implementation | Complexity |
|-----------|---------|-----------------|-----------|
| **Zero-shot Classification** | Sentiment | xlm-roberta-large-xnli | O(n·m) - n=tokens, m=model params |
| **Zero-shot Classification** | Category | facebook/bart-large-mnli | O(n·m) |
| **Keyword Matching** | Fallback Sentiment | SimpleSentimentAnalyzer | O(n·k) - k=keywords |
| **Time Bucketing** | Time Series | 6-hour intervals + TreeMap | O(n·log(t)) - t=time buckets |
| **Satisfaction Score** | Analysis | (positive-negative)/total | O(n) - linear aggregation |
| **Stream Processing** | Data Filtering | Java Streams API | O(n) |
| **Batch Processing** | ML Inference | Transformer batch inference | O(b·m) - b=batch size |

---

## 7. PERFORMANCE CHARACTERISTICS

- **First Run Installation**: 10-15 minutes (ML model download ~2GB)
- **Subsequent Runs**: 30 seconds (cached models)
- **Sentiment Analysis API**: 50-200ms per text (depends on text length)
- **Batch Analysis**: ~1-2 seconds for 31 posts
- **Database Queries**: <100ms (SQLite optimization)
- **UI Responsiveness**: Responsive charts with <500ms rendering

---

## 8. SCALABILITY NOTES

- **Current**: Designed for small-medium datasets (100s-1000s of posts)
- **Limitations**: 
  - Single-threaded UI (Java Swing)
  - SQLite (not ideal for >1M records)
  - Python API single-process Flask
- **Future Improvements**:
  - PostgreSQL/MySQL for large-scale data
  - Multi-threaded analysis processing
  - Gunicorn/uWSGI for Python API scaling
  - Caching layer (Redis) for frequent queries
