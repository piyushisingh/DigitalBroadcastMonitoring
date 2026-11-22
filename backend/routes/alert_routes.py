import uuid
import datetime
from flask import Blueprint, request, jsonify
from database import get_db, row_to_dict, rows_to_dict_list
from services.validation_service import VALID_SEVERITIES

alert_bp = Blueprint("alerts", __name__)


@alert_bp.route("/api/alerts", methods=["GET"])
def get_alerts():
    """Returns alerts with optional filtering by severity or channel."""
    conn = get_db()
    severity = request.args.get("severity")
    channel = request.args.get("channel")
    limit = request.args.get("limit")

    query = "SELECT * FROM alerts WHERE 1=1"
    params = []

    if severity:
        query += " AND UPPER(severity) = UPPER(?)"
        params.append(severity.strip())
    if channel:
        query += " AND LOWER(channel) = LOWER(?)"
        params.append(channel.strip())

    query += " ORDER BY created_at DESC"

    if limit:
        try:
            limit_val = int(limit)
            query += f" LIMIT {limit_val}"
        except ValueError:
            pass

    rows = conn.execute(query, params).fetchall()
    return jsonify(rows_to_dict_list(rows)), 200


@alert_bp.route("/api/alerts", methods=["POST"])
def create_alert():
    """Creates a new broadcast telemetry alert."""
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Request body must be a JSON object"}), 400

    title = data.get("title")
    description = data.get("description", "")
    severity = (data.get("severity") or "WARNING").upper()
    channel = data.get("channel")
    program_title = data.get("program_title") or data.get("programTitle", "")
    schedule_id = data.get("schedule_id") or data.get("scheduleId")
    is_simulated = 1 if bool(data.get("is_simulated") or data.get("isSimulated")) else 0

    if not title or not str(title).strip():
        return jsonify({"error": "Alert 'title' is required"}), 400
    if not channel or not str(channel).strip():
        return jsonify({"error": "Alert 'channel' is required"}), 400
    if severity not in VALID_SEVERITIES:
        return jsonify({"error": f"Invalid severity. Must be one of: {', '.join(sorted(VALID_SEVERITIES))}"}), 400

    alert_id = data.get("id") or f"alt-{uuid.uuid4().hex[:6]}"
    timestamp = data.get("timestamp") or datetime.datetime.now().strftime("%I:%M:%S %p IST")

    conn = get_db()
    conn.execute(
        """
        INSERT INTO alerts (id, timestamp, title, description, severity, channel, program_title, schedule_id, is_simulated)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (alert_id, timestamp, title.strip(), description.strip(), severity, channel.strip(), program_title, schedule_id, is_simulated)
    )
    conn.commit()

    created = conn.execute("SELECT * FROM alerts WHERE id = ?", (alert_id,)).fetchone()
    return jsonify(row_to_dict(created)), 201


@alert_bp.route("/api/alerts/<alert_id>", methods=["DELETE"])
def dismiss_alert(alert_id):
    """Dismisses (deletes) an alert by ID."""
    conn = get_db()
    existing = conn.execute("SELECT id FROM alerts WHERE id = ?", (alert_id,)).fetchone()
    if not existing:
        return jsonify({"error": f"Alert with id '{alert_id}' not found"}), 404

    conn.execute("DELETE FROM alerts WHERE id = ?", (alert_id,))
    conn.commit()
    return jsonify({"message": f"Alert '{alert_id}' successfully dismissed"}), 200
