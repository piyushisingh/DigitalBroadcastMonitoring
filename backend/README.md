# Digital Broadcast Monitoring - Backend API & ML Service

Python REST backend and machine-learning telemetry service for the native Android application **DigitalBroadcastMonitoring**.

---

## 1. Architecture & Technology Stack

- **Python Version**: 3.13.15 (64-bit)
- **Framework**: Flask 3.1.3
- **Database**: SQLite 3 (native `sqlite3` driver with foreign key constraints enabled)
- **Machine Learning**: `scikit-learn` 1.9.1 (IsolationForest anomaly detection), `numpy` 2.5.4, `joblib` 1.6.0
- **Testing**: Python `unittest` with Flask test client
- **Android Integration Target**: Base URL `http://10.0.2.2:5000` for Android Studio Emulator

---

## 2. Directory Layout

```
backend/
├── app.py                     # Flask application factory, CORS, and error handlers
├── database.py                # SQLite connection lifecycle, row_factory, init_db
├── schema.sql                 # Persistent tables (programs, schedules, alerts, status_log)
├── seed.py                    # Seeder matching 24 baseline programs & 8 schedules
├── train_model.py             # Standalone scikit-learn anomaly model trainer
├── requirements.txt           # Python dependencies
├── README.md                  # Complete backend documentation
├── .gitignore                 # Ignores .venv, caches, and local .db
├── models/
│   └── broadcast_anomaly_model.joblib # Serialized IsolationForest bundle
├── services/
│   ├── __init__.py
│   ├── validation_service.py # Chronology, overlap condition, and input validators
│   └── ml_service.py         # Feature extraction & scikit-learn inference engine
├── routes/
│   ├── __init__.py
│   ├── health_routes.py      # GET /api/health
│   ├── program_routes.py     # CRUD /api/programs
│   ├── schedule_routes.py    # CRUD /api/schedules & /api/schedules/<id>/analysis
│   ├── dashboard_routes.py   # Aggregated metrics computed from DB
│   ├── report_routes.py      # Durations, distributions, and analytics from DB
│   └── alert_routes.py       # CRUD & dismissal for /api/alerts
└── tests/
    ├── __init__.py
    ├── test_database.py           # Schema, foreign keys, CHECK constraints
    ├── test_programs.py           # Program CRUD and positive duration checks
    ├── test_schedules.py          # Schedule CRUD, conflicts, back-to-back, logs
    ├── test_dashboard_reports.py  # DB metrics calculation and dynamic updates
    ├── test_ml_service.py         # Feature extractor, model inference, analysis
    └── test_alerts_and_health.py  # Health and alert lifecycle tests
```

---

## 3. Windows Setup Instructions

### Step 3.1: Create Virtual Environment
Open PowerShell inside the repository root or `backend/` directory:
```powershell
# From repository root
py -3.13 -m venv backend\.venv
```

### Step 3.2: Activate Virtual Environment
```powershell
# In PowerShell:
.\backend\.venv\Scripts\Activate.ps1

# Or in Command Prompt:
backend\.venv\Scripts\activate.bat
```

### Step 3.3: Install Dependencies
```powershell
backend\.venv\Scripts\python.exe -m pip install -r backend\requirements.txt
```

---

## 4. Database Setup & Seeding

The SQLite database file `broadcast_monitoring.db` is initialized and populated with:
- 24 standardized baseline programs
- 8 initial broadcasts for today across DD National, DD Bharati, DD India, DD Sports HD, and DD Retro
- Telemetry alerts and status logs

To seed the database:
```powershell
backend\.venv\Scripts\python.exe backend\seed.py
```
Output:
```
Successfully seeded database at ...\backend\broadcast_monitoring.db with 24 programs and 8 schedules.
```

---

## 5. Machine Learning Model Training

The anomaly detector trains on broadcast schedule patterns (duration, start hour, end hour, prime-time status, and category mapping) using scikit-learn's `IsolationForest`.

> **Notice:** The model is trained on a synthetic demonstration dataset that models standard television broadcasting patterns alongside operational edge cases. It is intended for architecture demonstration and has not been validated on live production telemetry.

To train and serialize the model:
```powershell
backend\.venv\Scripts\python.exe backend\train_model.py
```
Output:
```
======================================================================
Digital Broadcast Monitoring - Anomaly Model Training
======================================================================
[1/4] Generating synthetic demonstration broadcast dataset...
      Total dataset size: 760 samples with 7 features.
[2/4] Training scikit-learn IsolationForest model...
      IsolationForest fitted successfully.
[3/4] Preparing output directory: backend\models
[4/4] Serializing model bundle to: backend\models\broadcast_anomaly_model.joblib
      Model successfully saved!
======================================================================
```

---

## 6. Running the Flask Server

Start the server on `0.0.0.0:5000`:
```powershell
backend\.venv\Scripts\python.exe backend\app.py
```

Console output:
```
Starting Digital Broadcast Monitoring Flask server on 0.0.0.0:5000...
Android Emulator base URL: http://10.0.2.2:5000
 * Serving Flask app 'app'
 * Debug mode: on
 * Running on all addresses (0.0.0.0)
 * Running on http://127.0.0.1:5000
```

---

## 7. Running Automated Tests

Run the complete isolated test suite:
```powershell
backend\.venv\Scripts\python.exe -m unittest discover -s backend/tests -p "test_*.py" -v
```

All 27 test cases run against isolated temporary SQLite databases and verify:
1. `test_database.py`: Table creation, foreign key enforcement, CHECK constraints.
2. `test_programs.py`: Full CRUD, missing fields rejection, positive duration validation.
3. `test_schedules.py`:
   - Valid schedule insertion.
   - Start < End chronology validation (`400 Bad Request`).
   - Overlap detection with `start1 < end2 AND start2 < end1` (`409 Conflict`).
   - Back-to-back broadcast allowance (`end1 == start2` succeeds with `201 Created`).
   - Different channels and different dates allowed at the same time.
   - Self-exclusion on update (editing details doesn't conflict with self).
   - Historical tracking in `status_log`.
4. `test_dashboard_reports.py`: Dashboard metrics and duration totals calculated strictly from DB rows.
5. `test_ml_service.py`: 1x7 feature extraction, IsolationForest inference, and `/api/schedules/<id>/analysis`.
6. `test_alerts_and_health.py`: Health check and alert query/dismissal lifecycle.

---

## 8. REST API Endpoint Reference

| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `GET` | `/api/health` | Health status of DB and ML model | `200` |
| `GET` | `/api/programs` | List programs (filter by `channel`, `category`, `search`) | `200` |
| `POST` | `/api/programs` | Create program (validates positive duration) | `201`, `400` |
| `GET` | `/api/programs/<id>` | Get program by ID | `200`, `404` |
| `PUT` | `/api/programs/<id>` | Update program by ID | `200`, `400`, `404` |
| `DELETE` | `/api/programs/<id>` | Delete program by ID (cascades schedules) | `200`, `404` |
| `GET` | `/api/schedules` | List schedules with joined program metadata | `200` |
| `POST` | `/api/schedules` | Create schedule (validates chronology & channel overlap) | `201`, `400`, `409` |
| `GET` | `/api/schedules/<id>` | Get schedule by ID | `200`, `404` |
| `PUT` | `/api/schedules/<id>` | Update schedule (excludes self from overlap check) | `200`, `400`, `409`, `404` |
| `DELETE` | `/api/schedules/<id>` | Delete schedule by ID | `200`, `404` |
| `GET` | `/api/schedules/<id>/analysis` | Combined deterministic rule check + ML anomaly score | `200`, `404` |
| `GET` | `/api/dashboard` | Live operational counts directly from DB | `200` |
| `GET` | `/api/reports` | Live broadcast hours, category & channel distribution | `200` |
| `GET` | `/api/alerts` | List alerts (filter by `severity`, `channel`, `limit`) | `200` |
| `POST` | `/api/alerts` | Create new telemetry alert | `201`, `400` |
| `DELETE` | `/api/alerts/<id>` | Dismiss alert by ID | `200`, `404` |

---

## 9. Conflict Validation Rule

To prevent conflicting broadcasts, the backend enforces:
```
start1 < end2 AND start2 < end1
```
Overlaps are checked **only** when both:
1. `broadcast_date` matches exactly.
2. `channel` (transmission feed) matches (case-insensitive).

### Back-to-Back Broadcasts
If Schedule A runs `10:00 - 11:00` and Schedule B runs `11:00 - 12:00`:
- `startA (600) < endB (720)` is TRUE.
- `startB (660) < endA (660)` is **FALSE**.
- Result: **Allowed** (HTTP 201 Created).

### Conflict Response (HTTP 409)
```json
{
  "error": "Conflicting broadcast schedule detected",
  "message": "Schedule conflicts with 'Morning News' (10:00 - 10:30) on channel DD National on date 2026-10-15.",
  "conflicting_schedule": {
    "id": "sch-101",
    "program_title": "Morning News",
    "start_time": "10:00",
    "end_time": "10:30"
  }
}
```

---

## 10. Android Emulator Connection Details

When connecting from the Android Studio Emulator:
- **Emulator Loopback IP**: `http://10.0.2.2:5000`
- **Why not `localhost`?** Inside an Android emulator, `localhost` (or `127.0.0.1`) refers to the Android device's own virtual network loopback interface, NOT your Windows development machine. The special alias `10.0.2.2` is routed by QEMU to your Windows host loopback.
- **Physical Device**: Use your machine's Wi-Fi / LAN IP (e.g., `http://192.168.x.x:5000`).
