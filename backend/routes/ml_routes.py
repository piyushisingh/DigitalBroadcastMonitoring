"""
Digital Broadcast Monitoring - Machine Learning API Routes

Provides endpoints for ML health readiness and early-session video-streaming
stalling risk inference.
"""

from flask import Blueprint, request, jsonify
from services.stalling_ml_service import get_ml_health_status, predict_stalling

ml_bp = Blueprint("ml", __name__)


@ml_bp.route("/api/ml/health", methods=["GET"])
def ml_health():
    """
    Returns the operational status, version, expected feature schema,
    and dataset provenance of the stalling ML model.
    """
    status_info = get_ml_health_status()
    code = 200 if status_info["is_ready"] else 503
    return jsonify(status_info), code


@ml_bp.route("/api/ml/stalling-risk", methods=["POST"])
def stalling_risk_prediction():
    """
    Accepts 7 early-session features, validates them, and invokes the persisted
    stalling prediction pipeline.
    """
    if not request.is_json:
        return jsonify({
            "error": "Bad request",
            "details": "Request body must be valid application/json"
        }), 400

    payload = request.get_json()
    result, error_msg, status_code = predict_stalling(payload)

    if error_msg:
        return jsonify({
            "error": "Inference error" if status_code >= 500 else "Validation error",
            "details": error_msg
        }), status_code

    return jsonify(result), 200
