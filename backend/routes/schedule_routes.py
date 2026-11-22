import uuid
from flask import Blueprint, request, jsonify
from database import get_db, row_to_dict, rows_to_dict_list
from services.validation_service import (
    validate_schedule_data,
    check_schedule_conflict,
    parse_time_to_minutes
)
from services.ml_service import run_inference

schedule_bp = Blueprint("schedules", __name__)


@schedule_bp.route("/api/schedules", methods=["GET"])
def get_schedules():
    """
    Returns schedules with optional filtering by date, channel, status, or program_id.
    Includes joined program metadata (program_title, category).
    """
    conn = get_db()
    date_param = request.args.get("date")
    channel_param = request.args.get("channel")
    status_param = request.args.get("status")
    program_param = request.args.get("program_id")

    query = """
        SELECT s.*, p.name as program_title, p.category as program_category,
               p.video_status, p.audio_status, p.compliance_score
        FROM schedules s
        JOIN programs p ON s.program_id = p.id
        WHERE 1=1
    """
    params = []

    if date_param:
        query += " AND s.broadcast_date = ?"
        params.append(date_param.strip())
    if channel_param:
        query += " AND LOWER(s.channel) = LOWER(?)"
        params.append(channel_param.strip())
    if status_param:
        query += " AND LOWER(s.status) = LOWER(?)"
        params.append(status_param.strip())
    if program_param:
        query += " AND s.program_id = ?"
        params.append(program_param.strip())

    query += " ORDER BY s.broadcast_date ASC, s.start_time ASC"
    rows = conn.execute(query, params).fetchall()
    return jsonify(rows_to_dict_list(rows)), 200


@schedule_bp.route("/api/schedules/<schedule_id>", methods=["GET"])
def get_schedule(schedule_id):
    """Returns a single schedule by ID with program metadata."""
    conn = get_db()
    query = """
        SELECT s.*, p.name as program_title, p.category as program_category,
               p.video_status, p.audio_status, p.compliance_score
        FROM schedules s
        JOIN programs p ON s.program_id = p.id
        WHERE s.id = ?
    """
    row = conn.execute(query, (schedule_id,)).fetchone()
    if not row:
        return jsonify({"error": f"Schedule with id '{schedule_id}' not found"}), 404
    return jsonify(row_to_dict(row)), 200


@schedule_bp.route("/api/schedules", methods=["POST"])
def create_schedule():
    """
    Creates a new schedule slot with strict server-side validation and overlap detection.
    Condition: start1 < end2 AND start2 < end1. Back-to-back broadcasts are allowed.
    Returns HTTP 409 if a conflict exists.
    """
    data = request.get_json(silent=True)
    cleaned, err = validate_schedule_data(data, is_update=False)
    if err:
        return jsonify({"error": err}), 400

    conn = get_db()

    # Validate program exists
    prog = conn.execute("SELECT id, name FROM programs WHERE id = ?", (cleaned["program_id"],)).fetchone()
    if not prog:
        return jsonify({"error": f"Program with id '{cleaned['program_id']}' does not exist"}), 400

    # Overlap detection check
    has_conflict, conflicting = check_schedule_conflict(
        channel=cleaned["channel"],
        broadcast_date=cleaned["broadcast_date"],
        start_time=cleaned["start_time"],
        end_time=cleaned["end_time"],
        db=conn
    )

    if has_conflict:
        return jsonify({
            "error": "Conflicting broadcast schedule detected",
            "message": (
                f"Schedule conflicts with '{conflicting['program_title']}' "
                f"({conflicting['start_time']} - {conflicting['end_time']}) "
                f"on channel {cleaned['channel']} on date {cleaned['broadcast_date']}."
            ),
            "conflicting_schedule": conflicting
        }), 409

    schedule_id = data.get("id") or f"sch-{uuid.uuid4().hex[:6]}"

    conn.execute(
        """
        INSERT INTO schedules (id, program_id, channel, broadcast_date, start_time, end_time, status, remarks, is_verified, has_overlap)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (
            schedule_id,
            cleaned["program_id"],
            cleaned["channel"],
            cleaned["broadcast_date"],
            cleaned["start_time"],
            cleaned["end_time"],
            cleaned.get("status", "Scheduled"),
            cleaned.get("remarks", ""),
            cleaned.get("is_verified", 1),
            0
        )
    )

    # Insert status log entry
    conn.execute(
        """
        INSERT INTO status_log (schedule_id, old_status, new_status, remarks)
        VALUES (?, ?, ?, ?)
        """,
        (schedule_id, None, cleaned.get("status", "Scheduled"), "Schedule created")
    )
    conn.commit()

    query = """
        SELECT s.*, p.name as program_title, p.category as program_category
        FROM schedules s
        JOIN programs p ON s.program_id = p.id
        WHERE s.id = ?
    """
    created = conn.execute(query, (schedule_id,)).fetchone()
    return jsonify(row_to_dict(created)), 201


@schedule_bp.route("/api/schedules/<schedule_id>", methods=["PUT"])
def update_schedule(schedule_id):
    """
    Updates an existing schedule. Checks conflict while excluding the schedule's own ID.
    Returns HTTP 409 on conflict.
    """
    conn = get_db()
    existing = conn.execute("SELECT * FROM schedules WHERE id = ?", (schedule_id,)).fetchone()
    if not existing:
        return jsonify({"error": f"Schedule with id '{schedule_id}' not found"}), 404

    data = request.get_json(silent=True)
    cleaned, err = validate_schedule_data(data, is_update=True)
    if err:
        return jsonify({"error": err}), 400

    if not cleaned:
        return jsonify({"error": "No valid fields provided to update"}), 400

    target_channel = cleaned.get("channel", existing["channel"])
    target_date = cleaned.get("broadcast_date", existing["broadcast_date"])
    target_start = cleaned.get("start_time", existing["start_time"])
    target_end = cleaned.get("end_time", existing["end_time"])

    # Overlap detection excluding self
    has_conflict, conflicting = check_schedule_conflict(
        channel=target_channel,
        broadcast_date=target_date,
        start_time=target_start,
        end_time=target_end,
        exclude_schedule_id=schedule_id,
        db=conn
    )

    if has_conflict:
        return jsonify({
            "error": "Conflicting broadcast schedule detected",
            "message": (
                f"Update conflicts with '{conflicting['program_title']}' "
                f"({conflicting['start_time']} - {conflicting['end_time']}) "
                f"on channel {target_channel} on date {target_date}."
            ),
            "conflicting_schedule": conflicting
        }), 409

    # Track status change in status_log
    old_status = existing["status"]
    new_status = cleaned.get("status")

    fields = []
    values = []
    for k, v in cleaned.items():
        fields.append(f"{k} = ?")
        values.append(v)

    fields.append("updated_at = datetime('now')")
    values.append(schedule_id)

    query = f"UPDATE schedules SET {', '.join(fields)} WHERE id = ?"
    conn.execute(query, values)

    if new_status and new_status != old_status:
        conn.execute(
            """
            INSERT INTO status_log (schedule_id, old_status, new_status, remarks)
            VALUES (?, ?, ?, ?)
            """,
            (schedule_id, old_status, new_status, f"Status updated from {old_status} to {new_status}")
        )

    conn.commit()

    updated = conn.execute(
        """
        SELECT s.*, p.name as program_title, p.category as program_category
        FROM schedules s
        JOIN programs p ON s.program_id = p.id
        WHERE s.id = ?
        """,
        (schedule_id,)
    ).fetchone()
    return jsonify(row_to_dict(updated)), 200


@schedule_bp.route("/api/schedules/<schedule_id>", methods=["DELETE"])
def delete_schedule(schedule_id):
    """Deletes a schedule by ID."""
    conn = get_db()
    existing = conn.execute("SELECT id FROM schedules WHERE id = ?", (schedule_id,)).fetchone()
    if not existing:
        return jsonify({"error": f"Schedule with id '{schedule_id}' not found"}), 404

    conn.execute("DELETE FROM schedules WHERE id = ?", (schedule_id,))
    conn.commit()
    return jsonify({"message": f"Schedule '{schedule_id}' successfully deleted"}), 200


@schedule_bp.route("/api/schedules/<schedule_id>/analysis", methods=["GET"])
def analyze_schedule(schedule_id):
    """
    Performs comprehensive broadcast telemetry analysis on a schedule slot:
    1. Deterministic rule-based validation (positive duration, chronology, channel overlap check).
    2. Machine learning anomaly inference using trained scikit-learn model.
    """
    conn = get_db()
    query = """
        SELECT s.*, p.name as program_title, p.category as program_category,
               p.video_status, p.audio_status, p.compliance_score
        FROM schedules s
        JOIN programs p ON s.program_id = p.id
        WHERE s.id = ?
    """
    schedule = conn.execute(query, (schedule_id,)).fetchone()
    if not schedule:
        return jsonify({"error": f"Schedule with id '{schedule_id}' not found"}), 404

    s_dict = row_to_dict(schedule)

    # 1. Deterministic Rule Validation
    rule_errors = []
    start_min = parse_time_to_minutes(s_dict["start_time"])
    end_min = parse_time_to_minutes(s_dict["end_time"])

    if start_min is None or end_min is None:
        rule_errors.append("Invalid time formatting")
    elif end_min <= start_min:
        rule_errors.append(f"Invalid interval: start ({s_dict['start_time']}) is not before end ({s_dict['end_time']})")

    duration_minutes = (end_min - start_min) if (start_min is not None and end_min is not None and end_min > start_min) else 0

    has_conflict, conflicting = check_schedule_conflict(
        channel=s_dict["channel"],
        broadcast_date=s_dict["broadcast_date"],
        start_time=s_dict["start_time"],
        end_time=s_dict["end_time"],
        exclude_schedule_id=schedule_id,
        db=conn
    )

    if has_conflict:
        rule_errors.append(
            f"Channel conflict: overlaps with '{conflicting['program_title']}' "
            f"({conflicting['start_time']} - {conflicting['end_time']})"
        )

    rule_validation = {
        "is_valid": len(rule_errors) == 0,
        "status": "PASS" if len(rule_errors) == 0 else "FAIL",
        "error_messages": rule_errors,
        "duration_minutes": duration_minutes,
        "has_overlap": has_conflict
    }

    # 2. ML Telemetry Anomaly Inference
    ml_result = run_inference(
        duration_minutes=duration_minutes,
        start_time=s_dict["start_time"],
        end_time=s_dict["end_time"],
        category=s_dict.get("program_category", "Other"),
        channel=s_dict["channel"]
    )

    return jsonify({
        "schedule": s_dict,
        "rule_validation": rule_validation,
        "ml_analysis": ml_result
    }), 200
