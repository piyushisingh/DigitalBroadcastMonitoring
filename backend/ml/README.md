# Early-Session Video Streaming QoE ML Pipeline

## Overview

This module provides a reproducible, leak-free machine-learning pipeline for **video-streaming Quality of Experience (QoE) prediction** in `DigitalBroadcastMonitoring`.

Given session metadata and downlink traffic measurements from the **initial 10 seconds** of playback, the model predicts whether the streaming session will experience **playback stalling** (`stalling_class`).

---

## 1. Dataset Provenance & Attribution

- **Dataset Name**: MUEXLab YouTube QoE Session Dataset (2020–2021)
- **Source / Attribution**: Multimedia Experience Laboratory (MUEXLab), Faculty of Electrical Engineering and Computing, University of Zagreb & University of Würzburg.
- **Local File Paths**:
  - Raw Data CSV: `backend/ml/data/raw/session-dataset.csv` (30,193 sessions, 1,940 videos)
  - Documentation: `backend/ml/data/raw/session-dataset-description.txt`
  - Audit Report: `backend/ml/data/audit_report.json`
- **Scope & Privacy**: The raw dataset contains third-party academic research measurements. The raw data folder `backend/ml/data/raw/` is explicitly added to `.gitignore` and is **never committed to Git** or bundled into the Android APK.

### Critical Engineering & Domain Disclaimer
> **IMPORTANT DOMAIN DISTINCTION**:
> This pipeline implements a **video-streaming QoE stalling prediction system**, **not** a direct physical television broadcast fault monitor.
>
> - It does **NOT** measure television transmitter RF faults, black-frame ratios, video frame cadence drops, or EBU R128 audio clipping.
> - The Android application does **NOT** automatically acquire live broadcast telemetry from RF hardware. The seven input features are video-session network characteristics that can be tested with example presets or user inputs.

---

## 2. Feature Schema & Selection Rationale

The primary model is strictly constrained to **seven early-session features** available during the first 10 seconds of playback:

| Feature Name | Type | Allowed / Expected Values | Units / Meaning |
| :--- | :--- | :--- | :--- |
| `year` | Categorical | `"2020"`, `"2021"` | Year of measurement collection |
| `location` | Categorical | `"Zagreb"`, `"Wuerzburg"` | Measurement laboratory site |
| `bandwidth` | Categorical | `"unlimited"`, `"1Mbps"`, `"stochastic"` | Network bottleneck throttling setting |
| `adblock` | Categorical | `"0"`, `"1"` | Adblock extension active (`"1"`) or inactive (`"0"`) |
| `dl_throughput_first1s` | Numeric | Non-negative float | Downlink average throughput in 1st second (Bytes/s) |
| `dl_throughput_first5s` | Numeric | Non-negative float | Downlink average throughput in first 5 seconds (Bytes/s) |
| `dl_throughput_first10s` | Numeric | Non-negative float | Downlink average throughput in first 10 seconds (Bytes/s) |

### Explicitly Excluded Columns
To strictly prevent target leakage and data snooping, the following columns are **never** used as predictors:
- `video`: Group identifier only (used strictly for group-isolated splitting).
- `stalling_class`: Prediction target.
- `MOS_p1203`, `startup_delay_class`, `avg_resolution_class`, `avg_bitrate_class`: Alternative estimation targets.
- All full-session traffic aggregations and packet distribution metrics beyond the first 10 seconds.

---

## 3. Data Leakage Prevention (Video-Group Splitting)

The dataset contains repeated measurement sessions across **1,940 distinct video titles** (`video` ID).

To prevent data leakage, the dataset is partitioned using group-aware splitting:
- **Seed**: `42`
- **Ratios**: 70% Train, 15% Validation, 15% Held-Out Test
- **Partition Sizes**:
  - **Training Set**: 21,179 sessions across 1,358 videos (Stalling rate: 38.24%)
  - **Validation Set**: 4,498 sessions across 291 videos (Stalling rate: 38.84%)
  - **Held-Out Test Set**: 4,516 sessions across 291 videos (Stalling rate: 38.29%)
- **Inter-Split Overlap**:
  - Train $\cap$ Validation Videos: **0**
  - Train $\cap$ Test Videos: **0**
  - Validation $\cap$ Test Videos: **0**

All preprocessing imputers, standardizers, derived feature transformers, and one-hot encoders are fitted **strictly on training data**.

---

## 4. Preprocessing & Feature Engineering Pipeline

Built using scikit-learn's `ColumnTransformer` and custom pipeline transformers:

1. **Throughput Dynamics Transformer (`EarlyThroughputDynamicsTransformer`)**:
   Engineered strictly from the 3 permitted throughput fields (`dl_throughput_first1s`, `dl_throughput_first5s`, `dl_throughput_first10s`) without requiring any additional API inputs:
   - `ratio_5_1`: Ratio of 5s throughput to 1s throughput ($\frac{T_5 + 1}{T_1 + 1}$), capturing early connection ramp-up.
   - `ratio_10_5`: Ratio of 10s throughput to 5s throughput ($\frac{T_{10} + 1}{T_5 + 1}$), capturing post-ramp stability or throttle decay.
   - `delta_10_1`: Linear throughput delta ($T_{10} - T_1$).
   - `t_min`: Minimum observed throughput across the 3 early sampling points.
   - `t_mean`: Average observed throughput across the 3 sampling points.
   - `log_t10`: Log1p of 10s throughput ($\log(1 + T_{10})$) to normalize high-throughput skew.

2. **Numerical Pipeline** (9 total numerical features after dynamics transform):
   - `SimpleImputer(strategy='median')`: Handles missing numerical inputs with training medians.
   - `StandardScaler()`: Standardizes numerical features to zero mean and unit variance.

3. **Categorical Pipeline** (`year`, `location`, `bandwidth`, `adblock`):
   - `SimpleImputer(strategy='most_frequent')`: Handles missing categoricals.
   - `OneHotEncoder(handle_unknown='ignore', sparse_output=False)`: Encodes categoricals cleanly.

---

## 5. Candidate Model Comparison (5-Fold Group CV & Validation Data)

Candidate estimators were evaluated using 5-fold video-group cross-validation on the training set and evaluated on the held-out validation partition (neither saw the final test set):

| Model Configuration | CV Macro F1 | Val Accuracy | Val Bal Acc | Val Macro F1 | Val ROC-AUC | Val Stalling Recall | Val Stalling Precision |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **DummyClassifier** (Most Frequent) | 0.3818 | 61.16% | 50.00% | 0.3795 | 0.5000 | 0.00% | 0.00% |
| **LogisticRegression** (Baseline) | 0.6662 | 69.68% | 66.72% | 0.6707 | 0.7682 | 53.46% | 62.90% |
| **ExtraTrees** (100 trees, Baseline) | 0.7668 | 79.50% | 77.96% | 0.7821 | 0.8623 | 71.04% | 74.89% |
| **RandomForest** (100 trees, Baseline) | 0.7694 | 79.57% | 78.04% | 0.7829 | 0.8653 | 71.21% | 74.94% |
| **HistGradientBoosting** (Baseline) | 0.7806 | 80.01% | 78.60% | 0.7881 | 0.8774 | 72.30% | 75.27% |
| **HistGradientBoosting** (+ Dynamics) | 0.7862 | 80.97% | 79.69% | 0.7985 | 0.8833 | 73.96% | 76.31% |
| **RandomForest** (d=16, n=200, + Dynamics) | 0.7872 | 80.95% | 79.51% | 0.7977 | 0.8823 | 73.10% | 76.74% |
| **RandomForest_Balanced_Tuned** *(Selected)* | **0.7885** | **81.04%** | **80.34%** | **0.8016** | **0.8826** | **77.22%** | **74.78%** |

Based on validation Macro F1 (**0.8016**) and Balanced Accuracy (**80.34%**), **`RandomForest_Balanced_Tuned`** (`n_estimators=200, max_depth=16, min_samples_leaf=2, class_weight='balanced_subsample'`) was selected as the production model (Version `1.1.0`).

---

## 6. Decision Threshold Analysis (Validation Set)

The decision threshold was tuned across probability steps $[0.20, 0.80]$ on the validation set:

| Threshold | Precision (Stalling) | Recall (Stalling) | Macro F1 | Balanced Accuracy | False Positive Rate |
| :---: | :---: | :---: | :---: | :---: | :---: |
| 0.20 | 56.66% | 93.47% | 0.6967 | 74.04% | 45.40% |
| 0.30 | 63.74% | 87.64% | 0.7569 | 77.99% | 31.66% |
| 0.40 | 69.98% | 82.31% | 0.7891 | 79.94% | 22.43% |
| 0.45 | 72.20% | 79.85% | 0.7956 | 80.17% | 19.52% |
| **0.50** *(Optimal)* | **74.78%** | **77.22%** | **0.8016** | **80.34%** | **16.54%** |
| 0.55 | 76.73% | 73.61% | 0.7993 | 79.72% | 14.18% |
| 0.60 | 78.98% | 69.26% | 0.7939 | 78.78% | 11.70% |
| 0.70 | 84.18% | 56.95% | 0.7623 | 75.08% | 6.80% |

**Selected Threshold**: `0.50` yields the peak Macro F1 score of `0.8016` with balanced precision (74.78%) and recall (77.22%).

---

## 7. Final Evaluation on Held-Out Test Set (Baseline vs. Improved)

The final test set (4,516 sessions, 291 unseen videos) was evaluated **once** after candidate selection:

| Metric | Baseline Model (v1.0.0) | Improved Model (v1.1.0) | Absolute Improvement |
| :--- | :---: | :---: | :---: |
| **Model Architecture** | `RandomForestClassifier(n=100)` | `RandomForestClassifier(n=200, depth=16, balanced)` + Dynamics | Regularized + Dynamics |
| **Accuracy** | 78.74% | **80.07%** | **+1.33%** |
| **Balanced Accuracy** | 76.74% | **79.09%** | **+2.35%** |
| **Macro F1 Score** | 0.7713 | **0.7899** | **+0.0186** |
| **ROC-AUC** | 0.8574 | **0.8728** | **+0.0154** |
| **PR-AUC** | 0.8136 | **0.8281** | **+0.0145** |
| **Stalling Precision** | 74.20% | **73.54%** | -0.66% |
| **Stalling Recall** | 68.19% | **74.90%** | **+6.71%** |
| **Stalling F1 Score** | 0.7107 | **0.7421** | **+0.0314** |
| **No-Stalling Precision** | 81.21% | **84.25%** | **+3.04%** |
| **No-Stalling Recall** | 85.29% | **83.28%** | -2.01% |
| **No-Stalling F1 Score** | 0.8320 | **0.8376** | **+0.0056** |
| **False Negatives** | 550 | **434** | **-116 missed stalls (-21.1%)** |

### Confusion Matrix Comparison
- **Baseline v1.0.0**:
  $$\begin{pmatrix} 2377 & 410 \\ 550 & 1179 \end{pmatrix}$$
- **Improved v1.1.0**:
  $$\begin{pmatrix} 2321 & 466 \\ 434 & 1295 \end{pmatrix}$$

The improved model successfully captures **116 additional stalling sessions** (a 21.1% reduction in critical false negatives) while simultaneously lifting overall accuracy to 80.07% and ROC-AUC to 0.8728.

---

## 8. Model Artifacts

Saved under `backend/ml/models/`:
- **`stalling_pipeline.joblib`**: The complete fitted scikit-learn Pipeline (preprocessing + dynamics transformer + imputer + scaler + one-hot encoder + RandomForestClassifier).
- **`stalling_metadata.json`**: Full provenance, feature definitions, validation comparisons, test metrics, decision threshold, and training timestamps.
- **`experiment_results.json`**: Detailed records of cross-validation, candidate comparisons, and threshold tuning sweeps.

---

## 9. Reproducible Command Reference (Windows PowerShell)

Run all commands from the repository root:

```powershell
# 1. Run Data Audit & Provenance Verification
.\backend\.venv\Scripts\python.exe backend/ml/audit_dataset.py

# 2. Run Comprehensive Model Experimentation & Tuning
.\backend\.venv\Scripts\python.exe backend/ml/experiment_tuning.py

# 3. Train and Persist Production Model Pipeline
.\backend\.venv\Scripts\python.exe backend/ml/train_pipeline.py

# 4. Standalone Pipeline Evaluation on Held-Out Test Data
.\backend\.venv\Scripts\python.exe backend/ml/evaluate_pipeline.py

# 5. Run Backend Test Suite (39 passing unit tests)
.\backend\.venv\Scripts\python.exe -m unittest discover -s backend/tests -p "test_*.py"

# 6. Run Android Unit Tests
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --no-daemon

# 7. Assemble Android Debug APK
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug --no-daemon
```

---

## 10. REST API Endpoints

### 1. `GET /api/ml/health`
Returns operational readiness, model version, and expected input schema.

**Response (200 OK)**:
```json
{
  "status": "READY",
  "is_ready": true,
  "model_name": "EarlySessionStallingClassifier",
  "model_version": "1.1.0",
  "selected_model": "RandomForestClassifier",
  "expected_input_fields": [
    {"name": "year", "type": "string", "allowed_values": ["2020", "2021"]},
    {"name": "location", "type": "string", "allowed_values": ["Wuerzburg", "Zagreb"]},
    {"name": "bandwidth", "type": "string", "allowed_values": ["unlimited", "1Mbps", "stochastic"]},
    {"name": "adblock", "type": "string_or_int", "allowed_values": ["0", "1"]},
    {"name": "dl_throughput_first1s", "type": "float", "unit": "Bytes/s"},
    {"name": "dl_throughput_first5s", "type": "float", "unit": "Bytes/s"},
    {"name": "dl_throughput_first10s", "type": "float", "unit": "Bytes/s"}
  ]
}
```

### 2. `POST /api/ml/stalling-risk`
Accepts a JSON payload containing the 7 early-session input fields.

**Request Payload**:
```json
{
  "year": "2020",
  "location": "Zagreb",
  "bandwidth": "unlimited",
  "adblock": "1",
  "dl_throughput_first1s": 2800000.0,
  "dl_throughput_first5s": 3400000.0,
  "dl_throughput_first10s": 4100000.0
}
```

**Response (200 OK)**:
```json
{
  "predicted_class": 0,
  "prediction_label": "no_stalling",
  "stalling_risk_score": 0.04,
  "decision_threshold": 0.50,
  "score_semantics": "uncalibrated_model_estimate",
  "model_name": "EarlySessionStallingClassifier",
  "model_version": "1.1.0",
  "estimator": "RandomForestClassifier",
  "latency_ms": 14.2,
  "timestamp": "2026-10-28T02:00:00Z",
  "provenance": {
    "dataset": "MUEXLab YouTube QoE Session Dataset",
    "attribution": "Multimedia Experience Laboratory (MUEXLab), University of Zagreb & University of Würzburg (2020-2021)",
    "disclaimer": "This model is trained on early-session video streaming QoE research data (initial 10s traffic)..."
  }
}
```

**Score Semantics**:
- `stalling_risk_score`: Represents an **uncalibrated ensemble voting estimate** output by `predict_proba()[:, 1]`. The decision threshold (`0.50`) is explicitly documented in the response payload and labeled in the UI as `"Uncalibrated model score (>50% risk threshold)"`.

---

## 11. Known System Limitations

1. **Research Traffic vs. Live Broadcast Hardware**: The model evaluates video-on-demand encrypted HTTP streaming sessions over IP networks. It does not interface with SDI/ASI television broadcast feeds, hardware demuxers, or terrestrial/satellite RF modulators.
2. **First 10s Horizon**: Throughput drops occurring later in long video sessions (e.g. after minute 5) cannot be detected from initial 10s telemetry alone.
3. **Bandwidth Categories**: The training set represents fixed laboratory throttle environments (`unlimited`, `1Mbps`, `stochastic`). Extreme multi-gigabit connections or cellular edge handovers are approximated under `unlimited` or `stochastic`.
