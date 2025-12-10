# Humanitarian Logistics Analysis System - User Guide

## Quick Start

### First Time Setup (10-15 minutes)
```bash
cd humanitarian-logistics
bash install.sh
```
This will:
- Check/install Java, Maven, Python
- Build the application
- Download ML models (~2GB)

### Run Application (Subsequent times)
```bash
cd humanitarian-logistics
bash run.sh
```

---

## Application Features

| Tab | Function |
|-----|----------|
| **📚 Data Collection** | Add posts manually, YouTube crawler, reset database |
| **📊 Use Our Database** | Load 31 curated posts for analysis |
| **📈 Problem 1** | Sentiment analysis per disaster category |
| **📉 Problem 2** | Temporal sentiment tracking over time |
| **🔍 Batch Analysis** | Analyze all posts with Python API |

---

## How to Use

1. **Load Sample Data:**
   - Click "📊 Use Our Database" → Load button

2. **Add Your Data:**
   - Click "📚 Data Collection" → Fill post details → Add Post

3. **Analyze:**
   - Click "🔍 Analyze All Posts"
   - Python API processes sentiment (0-1) and category

4. **View Results:**
   - Problem 1: Sentiment per category
   - Problem 2: Sentiment over time

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Python API won't start | Check: `python3 --version`, `cat .api.log`, port 5001 free |
| Java app crashes | Check: `java -version` (11+), database files exist |
| Model download timeout | Re-run `bash install.sh` |
| Port 5001 in use | `pkill -f "sentiment_api.py"` |

---

## System Requirements

- **Java:** 11+
- **Maven:** 3.6+
- **Python:** 3.8+
- **RAM:** 4GB minimum
- **Disk:** 5GB (for models)

---

## Database

```
humanitarian-logistics/data/
├── humanitarian_logistics_user.db      (your data)
└── humanitarian_logistics_curated.db   (sample posts)
```

---

## Stop Application

Press `Ctrl+C` - data saves automatically, API stops.

---

## FAQ

**Q: Do I run install.sh every time?**
A: No, only first time. Use `bash run.sh` afterwards.

**Q: How long does install.sh take?**
A: 10-15 minutes (downloads ~2GB ML models).

**Q: What if install fails?**
A: Check logs: `cat humanitarian-logistics/.api.log`, then re-run.

**Q: Can I move the folder?**
A: Yes, but re-run install.sh in new location.
