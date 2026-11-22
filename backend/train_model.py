"""
Digital Broadcast Monitoring - Anomaly Detection Model Trainer

DISCLAIMER:
This script generates a synthetic demonstration dataset for broadcast schedule telemetry
(duration, timing, and category patterns) and trains a scikit-learn IsolationForest model.
This model is intended for system demonstration and architecture validation only,
and has NOT been trained or validated on live broadcast production telemetry.
"""

import os
import sys
import datetime
import numpy as np
import joblib
from sklearn.ensemble import IsolationForest

# Ensure backend root is on sys.path
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from services.ml_service import extract_schedule_features, FEATURE_NAMES, MODEL_DIR, MODEL_PATH


def generate_synthetic_broadcast_data(n_normal=600, n_anomalies=50, random_seed=42):
    """
    Generates synthetic broadcast schedule data for demonstration purposes.

    Normal patterns represent typical television broadcasting:
      - Standard durations: 15, 30, 45, 60, 90, 120, 180 minutes
      - Daytime & primetime slots: 06:00 to 23:00
      - Realistic category combinations

    Anomalous patterns represent operational irregularities:
      - Wild durations: 6, 8, 420, 600 minutes
      - Extreme midnight overruns
      - Mismatched categories
    """
    rng = np.random.RandomState(random_seed)

    records = []

    # 1. Generate Normal Broadcast Patterns
    standard_durations = [15, 30, 45, 60, 90, 120, 180]
    categories = ["News", "Sports", "Entertainment", "Documentary", "Commercial", "Talk Show"]

    for _ in range(n_normal):
        cat = rng.choice(categories)
        if cat == "Commercial":
            dur = rng.choice([5, 10, 15])
        elif cat == "Sports":
            dur = rng.choice([90, 120, 180, 240])
        elif cat == "News":
            dur = rng.choice([15, 30, 60])
        else:
            dur = rng.choice([30, 45, 60, 90])

        start_h = rng.randint(6, 23)
        start_m = rng.choice([0, 15, 30, 45])
        start_min_total = start_h * 60 + start_m
        end_min_total = (start_min_total + dur) % 1440

        start_time = f"{start_h:02d}:{start_m:02d}"
        end_time = f"{end_min_total // 60:02d}:{end_min_total % 60:02d}"

        feat = extract_schedule_features(dur, start_time, end_time, cat)[0]
        records.append((feat, 1))  # 1 = Normal

    # 2. Generate Anomaly Broadcast Patterns
    for _ in range(n_anomalies):
        anomaly_type = rng.choice(["extreme_duration", "odd_slot", "ultra_short"])
        if anomaly_type == "extreme_duration":
            dur = rng.randint(360, 720)  # 6 to 12 hours news or talk show
            cat = rng.choice(["News", "Talk Show", "Documentary"])
            start_h = rng.randint(8, 20)
        elif anomaly_type == "odd_slot":
            dur = rng.randint(180, 300)
            cat = "Commercial"
            start_h = rng.randint(1, 4)
        else:
            dur = rng.randint(2, 6)
            cat = rng.choice(["Sports", "Documentary"])
            start_h = rng.randint(10, 21)

        start_m = rng.choice([0, 17, 33, 49])
        start_min_total = start_h * 60 + start_m
        end_min_total = (start_min_total + dur) % 1440
        start_time = f"{start_h:02d}:{start_m:02d}"
        end_time = f"{end_min_total // 60:02d}:{end_min_total % 60:02d}"

        feat = extract_schedule_features(dur, start_time, end_time, cat)[0]
        records.append((feat, -1))  # -1 = Outlier / Anomaly

    X = np.array([r[0] for r in records], dtype=np.float64)
    y = np.array([r[1] for r in records], dtype=np.int32)
    return X, y


def train_and_save_model():
    """
    Trains the IsolationForest anomaly detector and serializes it to disk.
    """
    print("=" * 70)
    print("Digital Broadcast Monitoring - Anomaly Model Training")
    print("=" * 70)
    print("DISCLAIMER: This model is trained on synthetic demonstration data.")
    print("It has NOT been validated on live broadcast operations.\n")

    print("[1/4] Generating synthetic demonstration broadcast dataset...")
    X, y = generate_synthetic_broadcast_data(n_normal=700, n_anomalies=60, random_seed=42)
    print(f"      Total dataset size: {X.shape[0]} samples with {X.shape[1]} features.")
    print(f"      Features: {', '.join(FEATURE_NAMES)}")

    print("\n[2/4] Training scikit-learn IsolationForest model...")
    # Train IsolationForest with contamination matching expected outlier ratio
    contamination = 60.0 / (700.0 + 60.0)
    model = IsolationForest(
        n_estimators=120,
        contamination=contamination,
        max_samples="auto",
        random_state=42,
        n_jobs=-1
    )
    model.fit(X)

    # Evaluate predictions on the synthetic demonstration set
    preds = model.predict(X)
    detected_anomalies = np.sum(preds == -1)
    print(f"      IsolationForest fitted successfully.")
    print(f"      Detected outliers in training demonstration data: {detected_anomalies}/{len(y)}")

    print(f"\n[3/4] Preparing output directory: {MODEL_DIR}")
    os.makedirs(MODEL_DIR, exist_ok=True)

    metadata = {
        "model_type": "IsolationForest",
        "n_estimators": 120,
        "features": FEATURE_NAMES,
        "trained_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "training_samples": int(X.shape[0]),
        "disclaimer": (
            "Demonstration model trained on synthetic broadcast features; "
            "not validated on live broadcast telemetry."
        )
    }

    bundle = {
        "model": model,
        "metadata": metadata
    }

    print(f"[4/4] Serializing model bundle to: {MODEL_PATH}")
    joblib.dump(bundle, MODEL_PATH)
    print("      Model successfully saved!")
    print("=" * 70)
    return MODEL_PATH


if __name__ == "__main__":
    train_and_save_model()
