"""
Digital Broadcast Monitoring - Stalling Inference Service

Manages model loading, in-memory caching, validation, and real-time inference
for the early-session video-streaming stalling prediction pipeline.
"""

import os
import sys
import time
import json
import datetime
import joblib

CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
BACKEND_DIR = os.path.dirname(CURRENT_DIR)
if BACKEND_DIR not in sys.path:
    sys.path.insert(0, BACKEND_DIR)

from ml.config import (
    MODEL_FILE_PATH,
    METADATA_FILE_PATH,
    INITIAL_FEATURES,
    ALLOWED_CATEGORICAL_VALUES,
    CLASS_NAMES,
    DATASET_PROVENANCE,
)
from ml.pipeline import validate_and_format_input

_cached_pipeline = None
_cached_metadata = None


def load_stalling_pipeline(force_reload=False):
    """
    Loads the serialized scikit-learn pipeline and metadata into memory.
    Returns (pipeline, metadata, error_message).
    """
    global _cached_pipeline, _cached_metadata

    if _cached_pipeline is not None and not force_reload:
        return _cached_pipeline, _cached_metadata, None

    if not os.path.exists(MODEL_FILE_PATH):
        return None, None, (
            f"Stalling model artifact not found at '{MODEL_FILE_PATH}'. "
            "Please run 'python backend/ml/train_pipeline.py' to train and persist the model."
        )

    try:
        pipeline = joblib.load(MODEL_FILE_PATH)
        metadata = {}
        if os.path.exists(METADATA_FILE_PATH):
            with open(METADATA_FILE_PATH, "r", encoding="utf-8") as f:
                metadata = json.load(f)

        _cached_pipeline = pipeline
        _cached_metadata = metadata
        return _cached_pipeline, _cached_metadata, None
    except Exception as e:
        return None, None, f"Failed to load stalling pipeline: {str(e)}"


def get_ml_health_status():
    """
    Returns readiness, model name, version, feature schema, and provenance.
    """
    pipeline, metadata, err = load_stalling_pipeline()
    is_ready = pipeline is not None

    expected_fields = [
        {
            "name": "year",
            "type": "string",
            "allowed_values": ALLOWED_CATEGORICAL_VALUES["year"],
            "description": "Measurement year ('2020' or '2021')",
        },
        {
            "name": "location",
            "type": "string",
            "allowed_values": ALLOWED_CATEGORICAL_VALUES["location"],
            "description": "Measurement site ('Wuerzburg' or 'Zagreb')",
        },
        {
            "name": "bandwidth",
            "type": "string",
            "allowed_values": ALLOWED_CATEGORICAL_VALUES["bandwidth"],
            "description": "Bandwidth restriction ('unlimited', '1Mbps', 'stochastic')",
        },
        {
            "name": "adblock",
            "type": "string_or_int",
            "allowed_values": ["0", "1"],
            "description": "Adblock active ('1') or inactive ('0')",
        },
        {
            "name": "dl_throughput_first1s",
            "type": "float",
            "unit": "Bytes/s",
            "description": "Average downlink throughput during 1st second",
        },
        {
            "name": "dl_throughput_first5s",
            "type": "float",
            "unit": "Bytes/s",
            "description": "Average downlink throughput during first 5 seconds",
        },
        {
            "name": "dl_throughput_first10s",
            "type": "float",
            "unit": "Bytes/s",
            "description": "Average downlink throughput during first 10 seconds",
        },
    ]

    return {
        "status": "READY" if is_ready else "UNAVAILABLE",
        "is_ready": is_ready,
        "error": err,
        "model_name": metadata.get("model_name", "EarlySessionStallingClassifier") if metadata else None,
        "model_version": metadata.get("model_version", "1.0.0") if metadata else None,
        "selected_model": metadata.get("selected_model") if metadata else None,
        "expected_input_fields": expected_fields,
        "target": {
            "name": "stalling_class",
            "mapping": CLASS_NAMES,
        },
        "provenance": {
            "dataset_name": DATASET_PROVENANCE["name"],
            "attribution": DATASET_PROVENANCE["attribution"],
            "disclaimer": DATASET_PROVENANCE["disclaimer"],
        },
    }


def predict_stalling(input_payload):
    """
    Validates input payload, performs model prediction, and returns structured risk response.

    Returns:
        (result_dict, error_message, http_status_code)
    """
    start_time = time.perf_counter()

    # 1. Validate inputs
    try:
        X_input = validate_and_format_input(input_payload)
    except ValueError as ve:
        return None, str(ve), 400

    # 2. Check model readiness
    pipeline, metadata, err = load_stalling_pipeline()
    if pipeline is None:
        return None, (err or "ML inference model is unavailable."), 503

    # 3. Perform model prediction
    try:
        decision_threshold = float(metadata.get("decision_threshold", 0.50)) if metadata else 0.50

        if hasattr(pipeline, "predict_proba"):
            probs = pipeline.predict_proba(X_input)[0]
            # Probability of stalling (class 1)
            risk_score = round(float(probs[1]), 4)
            raw_pred = 1 if risk_score >= decision_threshold else 0
        else:
            raw_pred = int(pipeline.predict(X_input)[0])
            risk_score = 1.0 if raw_pred == 1 else 0.0

        latency_ms = round((time.perf_counter() - start_time) * 1000, 2)
        prediction_label = CLASS_NAMES.get(raw_pred, "unknown")

        response = {
            "predicted_class": raw_pred,
            "prediction_label": prediction_label,
            "stalling_risk_score": risk_score,
            "decision_threshold": decision_threshold,
            "score_semantics": "uncalibrated_model_estimate",
            "score_description": (
                "Raw ensemble voting fraction from RandomForestClassifier.predict_proba(); "
                "represents relative stalling risk rather than a calibrated posterior probability."
            ),
            "model_name": metadata.get("model_name", "EarlySessionStallingClassifier"),
            "model_version": metadata.get("model_version", "1.1.0"),
            "estimator": metadata.get("selected_model", "RandomForest_Balanced_Tuned"),
            "latency_ms": latency_ms,
            "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
            "inputs": {
                "year": str(input_payload.get("year")),
                "location": str(input_payload.get("location")),
                "bandwidth": str(input_payload.get("bandwidth")),
                "adblock": str(input_payload.get("adblock")),
                "dl_throughput_first1s": float(input_payload.get("dl_throughput_first1s")),
                "dl_throughput_first5s": float(input_payload.get("dl_throughput_first5s")),
                "dl_throughput_first10s": float(input_payload.get("dl_throughput_first10s")),
            },
            "provenance": {
                "dataset": DATASET_PROVENANCE["name"],
                "attribution": DATASET_PROVENANCE["attribution"],
                "disclaimer": DATASET_PROVENANCE["disclaimer"],
            },
        }
        return response, None, 200

    except Exception as exc:
        return None, f"Error during model inference: {str(exc)}", 500
