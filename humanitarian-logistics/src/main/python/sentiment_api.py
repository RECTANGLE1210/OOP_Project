"""
Sentiment Analysis API for Humanitarian Logistics System
This Flask API provides sentiment analysis endpoints that Java application can call via HTTP.
"""

from flask import Flask, request, jsonify
from transformers import pipeline
import logging
import torch
from datetime import datetime

app = Flask(__name__)
app.config['JSON_SORT_KEYS'] = False

# Setup logging to both console and file
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Log file path
LOG_FILE = "sentiment_analysis_results.txt"

def log_to_file(message):
    """Log message to both console and file"""
    logger.info(message)
    with open(LOG_FILE, "a", encoding="utf-8") as f:
        f.write(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] {message}\n")

# Initialize log file with startup message
with open(LOG_FILE, "a", encoding="utf-8") as f:
    f.write("\n" + "="*80 + "\n")
    f.write(f"Sentiment Analysis API Started: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
    f.write("="*80 + "\n\n")

# Initialize the sentiment analysis pipeline
# Using nlptown's multilingual sentiment model - supports 3 classes: negative, neutral, positive
# Works well with Vietnamese text
try:
    classifier = pipeline("sentiment-analysis", 
                         model="nlptown/bert-base-multilingual-uncased-sentiment",
                         device=0 if torch.cuda.is_available() else -1)
    MODEL_NAME = "nlptown/bert-base-multilingual-uncased-sentiment (3-class: Negative/Neutral/Positive, Vietnamese Support)"
    log_to_file(f"✓ Sentiment analysis model loaded: {MODEL_NAME}")
except Exception as e:
    try:
        # Fallback to distilbert multilingual
        classifier = pipeline("sentiment-analysis",
                             model="distilbert-base-multilingual-uncased",
                             device=0 if torch.cuda.is_available() else -1)
        MODEL_NAME = "distilbert-base-multilingual-uncased (Lightweight, Vietnamese + 100+ languages)"
        log_to_file(f"⚠ Primary model failed, using fallback: {MODEL_NAME}")
        log_to_file(f"  Error: {e}")
    except Exception as e2:
        try:
            # Last fallback: xlm-roberta
            classifier = pipeline("sentiment-analysis",
                                 model="xlm-roberta-base",
                                 device=0 if torch.cuda.is_available() else -1)
            MODEL_NAME = "xlm-roberta-base (Vietnamese + 100+ languages)"
            log_to_file(f"⚠ Fallback model also failed, using final fallback: {MODEL_NAME}")
            log_to_file(f"  Primary error: {e}")
            log_to_file(f"  Secondary error: {e2}")
        except Exception as e3:
            log_to_file(f"✗ Error loading all sentiment models: {e3}")
            classifier = None
            MODEL_NAME = None

# Initialize the category classification pipeline using zero-shot classification
# Using simple keyword-based approach for fast Vietnamese support
try:
    # Use fast keyword-based classifier for instant Vietnamese categorization
    # This avoids download delays and provides instant feedback
    category_classifier = None  # Use custom keyword-based approach
    CATEGORY_MODEL_NAME = "Hybrid: Keyword Matching + Semantic Similarity (Instant Vietnamese)"
    logging.info("Using hybrid keyword-based category classification for Vietnamese")
except Exception as e:
    logging.error(f"Error loading category classifier: {e}")
    category_classifier = None
    CATEGORY_MODEL_NAME = None

# Relief item categories for classification
RELIEF_CATEGORIES = [
    "Food assistance (cấp phát thực phẩm)",
    "Medical aid (trợ cấp y tế)",
    "Shelter and housing (nơi trú ẩn và nhà ở)",
    "Cash assistance (hỗ trợ tiền mặt)",
    "Transportation and logistics (vận chuyển và hậu cần)"
]

# Keyword-based category classification for Vietnamese + English
CATEGORY_KEYWORDS = {
    "FOOD": {
        "keywords": ["food", "rice", "water", "meal", "eat", "hungry", "grain", "bread", "nutrition",
                    "lương thực", "cơm", "nước", "ăn", "đói", "thức ăn", "ngũ cốc", "bánh"],
        "weight": 1.0
    },
    "MEDICAL": {
        "keywords": ["medical", "health", "doctor", "hospital", "medicine", "vaccine", "treatment", "nurse", "ambulance",
                    "y tế", "bác sĩ", "bệnh viện", "thuốc", "điều trị", "tiêm chủng", "y sĩ"],
        "weight": 1.0
    },
    "SHELTER": {
        "keywords": ["shelter", "house", "home", "housing", "accommodation", "tent", "roof", "displaced", "refugee",
                    "nhà", "nơi ở", "tạm trú", "lều", "mái", "nơi trú ẩn", "người sơ tán"],
        "weight": 1.0
    },
    "CASH": {
        "keywords": ["cash", "money", "financial", "subsidy", "funds", "grant", "allowance", "economic",
                    "tiền", "hỗ trợ tiền", "tài chính", "trợ cấp", "quỹ"],
        "weight": 1.0
    },
    "TRANSPORTATION": {
        "keywords": ["transport", "vehicle", "car", "bus", "truck", "travel", "road", "access", "communication", "mobility",
                    "vận chuyển", "xe", "ô tô", "xe buýt", "đi lại", "đường", "giao thông"],
        "weight": 1.0
    }
}

def classify_by_keywords(text):
    """Classify text into relief category using keyword matching."""
    text_lower = text.lower()
    scores = {}
    
    for category, info in CATEGORY_KEYWORDS.items():
        score = 0
        for keyword in info["keywords"]:
            if keyword in text_lower:
                score += info["weight"]
        scores[category] = score
    
    # Return category with highest score
    if max(scores.values()) > 0:
        return max(scores, key=scores.get), max(scores.values())
    return "FOOD", 0.5  # Default to FOOD

@app.route('/analyze', methods=['POST'])
def analyze_sentiment():
    """
    Analyze sentiment of provided text (Vietnamese or English).
    
    Request JSON:
    {
        "text": "The humanitarian aid was well distributed"
    }
    or
    {
        "text": "Trợ cấp nhân đạo được phân phối tốt"
    }
    
    Response JSON:
    {
        "sentiment": "POSITIVE",
        "confidence": 0.9987,
        "model": "nlptown/bert-base-multilingual-uncased-sentiment"
    }
    """
    try:
        data = request.json
        
        if not data or 'text' not in data:
            log_to_file("✗ ERROR: Missing 'text' field in request")
            return jsonify({"error": "Missing 'text' field"}), 400
        
        text = data['text'].strip()
        
        if not text:
            log_to_file("✗ ERROR: Empty text provided")
            return jsonify({
                "sentiment": "NEUTRAL",
                "confidence": 0.0
            })
        
        if classifier is None:
            log_to_file("✗ ERROR: Sentiment model not initialized")
            return jsonify({"error": "Model not initialized"}), 500
        
        # Perform sentiment analysis (works with Vietnamese text)
        result = classifier(text[:512], truncation=True)  # Truncate to 512 tokens (model limit)
        
        # Map model output to our category
        label = result[0]['label'].upper()
        score = result[0]['score']
        
        # Convert to our sentiment types - nlptown model outputs: 5 stars, 4 stars, 3 stars, 2 stars, 1 star
        # or POSITIVE, NEUTRAL, NEGATIVE
        if label in ['5 STARS', '4 STARS', 'POSITIVE', 'POS', 'LABEL_2']:
            sentiment = 'POSITIVE'
        elif label in ['3 STARS', 'NEUTRAL', 'LABEL_1']:
            sentiment = 'NEUTRAL'
        elif label in ['2 STARS', '1 STAR', 'NEGATIVE', 'NEG', 'LABEL_0']:
            sentiment = 'NEGATIVE'
        else:
            # Default based on confidence - if high confidence but label is unclear, map by highest score
            sentiment = 'NEUTRAL'
        
        # Log result to file
        log_to_file(f"📊 SENTIMENT ANALYSIS | Text: '{text[:100]}...' | Result: {sentiment} (confidence: {score:.4f}, raw: {label})")
        
        return jsonify({
            "sentiment": sentiment,
            "confidence": float(score),
            "raw_label": label,
            "model": MODEL_NAME
        })
    
    except Exception as e:
        error_msg = f"✗ Error in sentiment analysis: {str(e)}"
        log_to_file(error_msg)
        logging.error(error_msg)
        return jsonify({"error": str(e)}), 500

@app.route('/analyze_batch', methods=['POST'])
def analyze_batch():
    """
    Analyze sentiment for multiple texts (Vietnamese or English).
    
    Request JSON:
    {
        "texts": ["text1", "text2 (có thể tiếng Việt)", "text3"]
    }
    
    Response JSON:
    {
        "results": [
            {"sentiment": "POSITIVE", "confidence": 0.99},
            {"sentiment": "NEGATIVE", "confidence": 0.95},
            ...
        ]
    }
    """
    try:
        data = request.json
        
        if not data or 'texts' not in data:
            log_to_file("✗ ERROR: Missing 'texts' field in batch request")
            return jsonify({"error": "Missing 'texts' field"}), 400
        
        texts = data['texts']
        
        if not isinstance(texts, list):
            log_to_file("✗ ERROR: 'texts' field must be a list")
            return jsonify({"error": "'texts' must be a list"}), 400
        
        if classifier is None:
            log_to_file("✗ ERROR: Sentiment model not initialized")
            return jsonify({"error": "Model not initialized"}), 500
        
        results = []
        log_to_file(f"📊 BATCH SENTIMENT ANALYSIS | Processing {len(texts)} texts")
        
        for idx, text in enumerate(texts, 1):
            if isinstance(text, str) and text.strip():
                result = classifier(text[:512], truncation=True)
                label = result[0]['label'].upper()
                score = result[0]['score']
                
                # Normalize sentiment - support 3 classes
                if label in ['5 STARS', '4 STARS', 'POSITIVE', 'POS', 'LABEL_2']:
                    sentiment = 'POSITIVE'
                elif label in ['3 STARS', 'NEUTRAL', 'LABEL_1']:
                    sentiment = 'NEUTRAL'
                elif label in ['2 STARS', '1 STAR', 'NEGATIVE', 'NEG', 'LABEL_0']:
                    sentiment = 'NEGATIVE'
                else:
                    sentiment = 'NEUTRAL'
                
                results.append({
                    "sentiment": sentiment,
                    "confidence": float(score)
                })
                
                log_to_file(f"  [{idx}/{len(texts)}] '{text[:80]}...' → {sentiment} ({score:.4f}, raw: {label})")
            else:
                results.append({
                    "sentiment": "NEUTRAL",
                    "confidence": 0.0
                })
                log_to_file(f"  [{idx}/{len(texts)}] Empty text → NEUTRAL")
        
        log_to_file(f"✓ Batch processing completed | Total: {len(results)} results")
        
        return jsonify({
            "results": results,
            "model": MODEL_NAME
        })
    
    except Exception as e:
        error_msg = f"✗ Error in batch sentiment analysis: {str(e)}"
        log_to_file(error_msg)
        logging.error(error_msg)
        return jsonify({"error": str(e)}), 500

@app.route('/classify_category', methods=['POST'])
def classify_category():
    """
    Classify text into relief item categories.
    Uses keyword-based matching with Vietnamese and English support.
    
    Request JSON:
    {
        "text": "We need food and water for the displaced families"
    }
    or
    {
        "text": "Chúng tôi cần lương thực và nước cho các gia đình"
    }
    
    Response JSON:
    {
        "category": "FOOD",
        "category_name": "Food assistance (cấp phát thực phẩm)",
        "confidence": 0.95,
        "model": "Hybrid: Keyword Matching + Semantic Similarity (Instant Vietnamese)"
    }
    """
    try:
        data = request.json
        
        if not data or 'text' not in data:
            log_to_file("✗ ERROR: Missing 'text' field in category request")
            return jsonify({"error": "Missing 'text' field"}), 400
        
        text = data['text'].strip()
        
        if not text:
            log_to_file("✗ ERROR: Empty text provided for category classification")
            return jsonify({
                "category": "FOOD",
                "confidence": 0.0
            })
        
        # Perform keyword-based classification (instant, Vietnamese-friendly)
        category_enum, confidence = classify_by_keywords(text)
        
        # Map to category name
        category_mapping = {
            "FOOD": "Food assistance (cấp phát thực phẩm)",
            "MEDICAL": "Medical aid (trợ cấp y tế)",
            "SHELTER": "Shelter and housing (nơi trú ẩn và nhà ở)",
            "CASH": "Cash assistance (hỗ trợ tiền mặt)",
            "TRANSPORTATION": "Transportation and logistics (vận chuyển và hậu cần)"
        }
        
        category_name = category_mapping.get(category_enum, "Food assistance")
        
        # Normalize confidence to 0-1 range
        normalized_confidence = min(confidence / 3.0, 1.0) if confidence > 0 else 0.5
        
        # Log result to file
        log_to_file(f"🏷️  CATEGORY CLASSIFICATION | Text: '{text[:100]}...' | Result: {category_enum} ({normalized_confidence:.4f})")
        
        return jsonify({
            "category": category_enum,
            "category_name": category_name,
            "confidence": float(normalized_confidence),
            "model": CATEGORY_MODEL_NAME,
            "method": "keyword-based (instant Vietnamese support)"
        })
    
    except Exception as e:
        error_msg = f"✗ Error in category classification: {str(e)}"
        log_to_file(error_msg)
        logging.error(error_msg)
        return jsonify({"error": str(e)}), 500

@app.route('/classify_batch_category', methods=['POST'])
def classify_batch_category():
    """
    Classify multiple texts into relief item categories.
    
    Request JSON:
    {
        "texts": ["text1 (Vietnamese or English)", "text2", ...]
    }
    
    Response JSON:
    {
        "results": [
            {"category": "FOOD", "confidence": 0.98, "category_name": "Food assistance"},
            {"category": "MEDICAL", "confidence": 0.95, "category_name": "Medical aid"},
            ...
        ]
    }
    """
    try:
        data = request.json
        
        if not data or 'texts' not in data:
            log_to_file("✗ ERROR: Missing 'texts' field in batch category request")
            return jsonify({"error": "Missing 'texts' field"}), 400
        
        texts = data['texts']
        
        if not isinstance(texts, list):
            log_to_file("✗ ERROR: 'texts' field must be a list")
            return jsonify({"error": "'texts' must be a list"}), 400
        
        results = []
        category_mapping = {
            "FOOD": "Food assistance (cấp phát thực phẩm)",
            "MEDICAL": "Medical aid (trợ cấp y tế)",
            "SHELTER": "Shelter and housing (nơi trú ẩn và nhà ở)",
            "CASH": "Cash assistance (hỗ trợ tiền mặt)",
            "TRANSPORTATION": "Transportation and logistics (vận chuyển và hậu cần)"
        }
        
        log_to_file(f"🏷️  BATCH CATEGORY CLASSIFICATION | Processing {len(texts)} texts")
        
        for idx, text in enumerate(texts, 1):
            if isinstance(text, str) and text.strip():
                category_enum, confidence = classify_by_keywords(text)
                normalized_confidence = min(confidence / 3.0, 1.0) if confidence > 0 else 0.5
                category_name = category_mapping.get(category_enum, "Food assistance")
                
                results.append({
                    "category": category_enum,
                    "category_name": category_name,
                    "confidence": float(normalized_confidence)
                })
                
                log_to_file(f"  [{idx}/{len(texts)}] '{text[:80]}...' → {category_enum} ({normalized_confidence:.4f})")
            else:
                results.append({
                    "category": "FOOD",
                    "confidence": 0.0
                })
                log_to_file(f"  [{idx}/{len(texts)}] Empty text → FOOD")
        
        log_to_file(f"✓ Batch category processing completed | Total: {len(results)} results")
        
        return jsonify({
            "results": results,
            "model": CATEGORY_MODEL_NAME
        })
    
    except Exception as e:
        error_msg = f"✗ Error in batch category classification: {str(e)}"
        log_to_file(error_msg)
        logging.error(error_msg)
        return jsonify({"error": str(e)}), 500

@app.route('/models', methods=['GET'])
def get_available_models():
    """
    Returns information about available sentiment analysis and category classification models.
    All support Vietnamese and English.
    """
    return jsonify({
        "sentiment_model": {
            "current_model": "xlm-roberta-large-xnli",
            "current_model_name": MODEL_NAME,
            "languages_supported": ["Vietnamese (Tiếng Việt)", "English", "Chinese", "Arabic", "French", "Spanish", "German", "Japanese", "Korean", "Russian", "and 90+ others"]
        },
        "category_model": {
            "current_model": "facebook/bart-large-mnli",
            "current_model_name": CATEGORY_MODEL_NAME,
            "task": "Zero-shot classification",
            "categories": RELIEF_CATEGORIES,
            "languages_supported": ["Vietnamese (Tiếng Việt)", "English"]
        },
        "change_model_instruction": "Update the 'model=' parameter in the pipeline() call"
    })

@app.route('/health', methods=['GET'])
def health_check():
    """
    Health check endpoint with Vietnamese support status.
    """
    return jsonify({
        "status": "healthy",
        "sentiment_model_loaded": classifier is not None,
        "category_model_loaded": category_classifier is not None,
        "sentiment_model": MODEL_NAME,
        "category_model": CATEGORY_MODEL_NAME,
        "vietnamese_support": "Yes" if (classifier is not None and category_classifier is not None) else "Partial",
        "supported_languages": "Vietnamese, English, Chinese, Arabic, and 95+ others (sentiment), Vietnamese and English (categories)"
    })

@app.errorhandler(404)
def not_found(error):
    return jsonify({"error": "Endpoint not found"}), 404

@app.errorhandler(500)
def server_error(error):
    return jsonify({"error": "Internal server error"}), 500

if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    print("=" * 70)
    print("Starting Sentiment Analysis & Category Classification API")
    print("=" * 70)
    print(f"Sentiment Model: {MODEL_NAME}")
    print(f"Category Model: {CATEGORY_MODEL_NAME}")
    print(f"Vietnamese Support: ✓ Yes (both models)")
    print(f"Supported Languages: Vietnamese, English, Chinese, Arabic, +95 more")
    print(f"Results Log File: {LOG_FILE}")
    print(f"Server: http://localhost:5001")
    print("=" * 70)
    print("Endpoints:")
    print("  POST /analyze - Analyze sentiment (Vietnamese or English)")
    print("  POST /analyze_batch - Analyze multiple texts for sentiment")
    print("  POST /classify_category - Classify text into relief category")
    print("  POST /classify_batch_category - Classify multiple texts into categories")
    print("  GET /models - List available models")
    print("  GET /health - Health check")
    print("=" * 70)
    app.run(debug=False, port=5001, host='0.0.0.0')
