#!/bin/bash
# Run Humanitarian Logistics Application
# Integrated Python API + Java App - All-in-one startup

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_DIR" || exit 1

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Global variables for cleanup
PYTHON_API_PID=""
JAVA_APP_PID=""

# Function to print colored output
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# Cleanup function - called on EXIT or Ctrl+C
cleanup() {
    echo ""
    echo "========================================"
    echo "Shutting down..."
    echo "========================================"
    
    # Kill Java app if still running
    if [ -n "$JAVA_APP_PID" ] && kill -0 "$JAVA_APP_PID" 2>/dev/null; then
        print_info "Stopping Java application (PID: $JAVA_APP_PID)..."
        kill "$JAVA_APP_PID" 2>/dev/null
        sleep 1
    fi
    
    # Kill Python API if still running
    if [ -n "$PYTHON_API_PID" ] && kill -0 "$PYTHON_API_PID" 2>/dev/null; then
        print_info "Stopping Python API (PID: $PYTHON_API_PID)..."
        kill "$PYTHON_API_PID" 2>/dev/null
        sleep 1
    fi
    
    # Force kill any remaining processes
    pkill -f "sentiment_api.py" 2>/dev/null || true
    pkill -f "HumanitarianLogisticsApp" 2>/dev/null || true
    
    print_status "All processes stopped"
}

# Setup trap for cleanup on EXIT or INT (Ctrl+C)
trap cleanup EXIT INT TERM

# Header
echo "========================================"
echo "Humanitarian Logistics Analysis System"
echo "========================================"
echo "Integrated: Python API + Java Application"
echo ""

# ============================================
# STEP 1: Start Python API
# ============================================
echo ""
print_info "STEP 1: Starting Python API for Sentiment & Category Classification"
echo "========================================"

if command -v python3 &> /dev/null; then
    PYTHON_CMD="python3"
elif command -v python &> /dev/null; then
    PYTHON_CMD="python"
else
    print_error "Python not found. Please run install.sh first to set up dependencies."
    exit 1
fi

# Kill any existing API process
pkill -f "sentiment_api.py" 2>/dev/null || true
sleep 1

PYTHON_API_SCRIPT="$APP_DIR/src/main/python/sentiment_api.py"
if [ -f "$PYTHON_API_SCRIPT" ]; then
    print_info "Starting Python API (with UTF-8 encoding support)..."
    
    # Start API in background
    $PYTHON_CMD "$PYTHON_API_SCRIPT" > "$APP_DIR/.api.log" 2>&1 &
    PYTHON_API_PID=$!
    
    # Wait for API to start (wait indefinitely until it's ready)
    print_info "Waiting for API initialization..."
    API_READY=0
    TOTAL_WAIT=0
    MAX_WAIT=600  # Maximum 10 minutes
    
    while [ $API_READY -eq 0 ] && [ $TOTAL_WAIT -lt $MAX_WAIT ]; do
        if curl -s http://localhost:5001/health > /dev/null 2>&1; then
            API_READY=1
            break
        fi
        TOTAL_WAIT=$((TOTAL_WAIT + 1))
        echo -n "."
        sleep 1
    done
    
    echo ""
    
    if [ $API_READY -eq 1 ]; then
        print_status "Python API ready on http://localhost:5001 (PID: $PYTHON_API_PID)"
        print_info "Sentiment Model: Vietnamese + 100+ languages"
        print_info "Category Model: Keyword-based (instant Vietnamese)"
    else
        print_error "Python API failed to start after ${MAX_WAIT} seconds!"
        print_info "Check logs: cat $APP_DIR/.api.log"
        exit 1
    fi
else
    print_error "sentiment_api.py not found at $PYTHON_API_SCRIPT"
    exit 1
fi

# ============================================
# STEP 2: Build and Start Java Application
# ============================================
echo ""
print_info "STEP 2: Building and Starting Java Application"
echo "========================================"

if mvn clean compile package -DskipTests -q 2>/dev/null; then
    print_status "Build successful!"
else
    print_error "Build failed!"
    exit 1
fi

echo ""
echo "📱 Application Features:"
echo "  • 📚 Data Collection: Add posts manually"
echo "  • 📊 Use Our Database: Load 31 curated posts"
echo "  • 📈 Problem 1: Satisfaction analysis per category"
echo "  • 📉 Problem 2: Temporal sentiment tracking"
echo "  • 🔍 Batch Analysis: Analyze all posts with Python API"
echo ""
echo "🧠 ML Models:"
echo "  • Sentiment: xlm-roberta (Vietnamese + 100+ languages)"
echo "  • Categories: Keyword-based (instant Vietnamese)"
echo ""
echo "🔧 API Connection:"
echo "  • Status: Connected to http://localhost:5001"
echo "  • Response Format: JSON with sentiment + confidence"
echo ""
echo "💡 Tip: Click '🔍 Analyze All Posts' in Analysis tab to run sentiment analysis"
echo ""
print_info "Java application starting in 2 seconds..."
sleep 2

# Start Java application using the JAR with dependencies
JAR_FILE="target/humanitarian-logistics-1.0-SNAPSHOT-jar-with-dependencies.jar"
if [ ! -f "$JAR_FILE" ]; then
    print_error "JAR file not found! Build may have failed."
    exit 1
fi

print_info "Starting Java application..."
java -jar "$JAR_FILE" &
JAVA_APP_PID=$!

# Wait for Java app to exit
wait $JAVA_APP_PID 2>/dev/null

# If we get here, Java app has exited
print_info "Java application closed"
