import os
import time
import numpy as np

# Path to the serialized ML model
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL_DIR = os.path.join(BASE_DIR, "models")
MODEL_PATH = os.path.join(MODEL_DIR, "broadcast_anomaly_model.joblib")

CATEGORY_MAPPING = {
    "news": 1,
    "sports": 2,
    "entertainment": 3,
    "documentary": 4,
    "commercial": 5,
    "talk show": 6,
    "talk_show": 6,
    "other": 0
}

FEATURE_NAMES = [
    "duration_minutes",
    "start_hour",
    "start_minute",
    "end_hour",
    "is_prime_time",
    "is_overnight",
    "category_code"
]

_cached_model = None
_cached_metadata = None


def extract_schedule_features(duration_minutes, start_time_str, end_time_str, category_str="Other"):
    """
    Extracts numerical feature vector for broadcast anomaly detection.
    Features:
      1. duration_minutes (e.g. 30, 60, 120)
      2. start_hour (0-23)
      3. start_minute (0-59)
      4. end_hour (0-23)
      5. is_prime_time (1 if 19 <= start_hour <= 22 else 0)
      6. is_overnight (1 if start_hour >= 23 or start_hour < 6 else 0)
      7. category_code (integer mapping)
    """
    from services.validation_service import parse_time_to_minutes

    start_min = parse_time_to_minutes(start_time_str) or 0
    end_min = parse_time_to_minutes(end_time_str) or 0

    start_hour = start_min // 60
    start_minute = start_min % 60
    end_hour = end_min // 60

    is_prime = 1 if 19 <= start_hour <= 22 else 0
    is_overnight = 1 if (start_hour >= 23 or start_hour < 6) else 0

    cat_clean = str(category_str).strip().lower()
    cat_code = CATEGORY_MAPPING.get(cat_clean, 0)

    feature_vec = [
        float(duration_minutes),
        float(start_hour),
        float(start_minute),
        float(end_hour),
        float(is_prime),
        float(is_overnight),
        float(cat_code)
    ]
    return np.array([feature_vec], dtype=np.float64)


def load_model(force_reload=False):
    """
    Loads the trained scikit-learn anomaly-detection model bundle if available.
    Returns (model_bundle, error_message).
    """
    global _cached_model, _cached_metadata

    if _cached_model is not None and not force_reload:
        return {"model": _cached_model, "metadata": _cached_metadata}, None

    if not os.path.exists(MODEL_PATH):
        return None, (
            f"Model file not found at '{MODEL_PATH}'. "
            "Please run 'python train_model.py' to generate and train the demonstration anomaly model."
        )

    try:
        import joblib
        bundle = joblib.load(MODEL_PATH)
        _cached_model = bundle.get("model")
        _cached_metadata = bundle.get("metadata", {})
        return {"model": _cached_model, "metadata": _cached_metadata}, None
    except Exception as e:
        return None, f"Failed to load anomaly detection model: {str(e)}"


def run_inference(duration_minutes, start_time, end_time, category="Other", channel="DD National"):
    """
    Performs anomaly inference using the trained scikit-learn model.
    If the model is not found, returns a structured report with model_loaded = False,
    never silently substituting fake model predictions.
    """
    start_perf = time.perf_counter()

    bundle, load_err = load_model()
    if bundle is None:
        return {
            "model_loaded": False,
            "model_status": "NOT_TRAINED",
            "message": load_err,
            "anomaly_score": None,
            "is_anomaly": None,
            "latency_ms": round((time.perf_counter() - start_perf) * 1000, 2),
            "disclaimer": "No trained model file present. Run train_model.py to initialize."
        }

    model = bundle["model"]
    metadata = bundle.get("metadata", {})

    features = extract_schedule_features(duration_minutes, start_time, end_time, category)

    # Scikit-learn IsolationForest prediction: -1 = anomaly, 1 = normal
    raw_pred = model.predict(features)[0]
    # Decision function: lower values mean more anomalous (typically around 0 is boundary)
    raw_score = model.decision_function(features)[0]

    # Map raw decision function to normalized anomaly score in [0.0, 1.0]
    # In IsolationForest, score near 0 or negative is anomalous; positive > 0.15 is normal.
    normalized_score = float(np.clip(1.0 / (1.0 + np.exp(raw_score * 8.0)), 0.0, 1.0))
    is_anomaly = bool(raw_pred == -1 or normalized_score > 0.60)

    latency_ms = round((time.perf_counter() - start_perf) * 1000, 2)

    # Contextual diagnostic insights
    detected_anomalies = []
    cat_lower = str(category).lower()

    if duration_minutes > 180 and "sports" not in cat_lower:
        detected_anomalies.append(f"Excessive duration ({duration_minutes}m) for non-sports category '{category}'")
    elif duration_minutes < 15 and "commercial" not in cat_lower:
        detected_anomalies.append(f"Atypically short slot ({duration_minutes}m) for category '{category}'")

    from services.validation_service import parse_time_to_minutes
    s_min = parse_time_to_minutes(start_time) or 0
    s_hour = s_min // 60
    if (s_hour >= 23 or s_hour < 5) and duration_minutes > 60:
        detected_anomalies.append(f"Extended late-night transmission slot ({duration_minutes}m at {start_time})")

    if is_anomaly and not detected_anomalies:
        detected_anomalies.append("Statistical feature distribution deviation identified by IsolationForest")

    compliance_prediction = "Action Required: Telemetry deviation" if is_anomaly else "Optimal Compliance (>98%)"

    return {
        "model_loaded": True,
        "model_status": "READY",
        "model_type": metadata.get("model_type", "IsolationForest"),
        "is_anomaly": is_anomaly,
        "anomaly_score": round(normalized_score, 4),
        "raw_decision_score": round(float(raw_score), 4),
        "latency_ms": latency_ms,
        "compliance_prediction": compliance_prediction,
        "detected_anomalies": detected_anomalies if is_anomaly else ["No schedule telemetry anomalies detected"],
        "disclaimer": metadata.get(
            "disclaimer",
            "Demonstration model trained on synthetic broadcast features; not validated on live broadcast telemetry."
        )
    }
