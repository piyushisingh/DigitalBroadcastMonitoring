import datetime
from flask import Blueprint, jsonify
from database import get_db
from services.ml_service import load_model

health_bp = Blueprint("health", __name__)


@health_bp.route("/api/health", methods=["GET"])
def health_check():
    """
    Health check endpoint returning system status, database connectivity,
    and ML model readiness.
    """
    # Check DB
    db_status = "healthy"
    try:
        conn = get_db()
        conn.execute("SELECT 1").fetchone()
    except Exception as e:
        db_status = f"unhealthy: {str(e)}"

    # Check ML Model
    bundle, err = load_model()
    model_status = "ready" if bundle is not None else "not_trained"

    return jsonify({
        "status": "ok",
        "service": "DigitalBroadcastMonitoring-Backend",
        "version": "1.0.0",
        "database": db_status,
        "ml_model": model_status,
        "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat()
    }), 200
