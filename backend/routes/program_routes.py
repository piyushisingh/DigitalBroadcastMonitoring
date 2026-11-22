import uuid
from flask import Blueprint, request, jsonify
from database import get_db, row_to_dict, rows_to_dict_list
from services.validation_service import validate_program_data

program_bp = Blueprint("programs", __name__)


@program_bp.route("/api/programs", methods=["GET"])
def get_programs():
    """Returns all programs, with optional filtering by channel or category."""
    conn = get_db()
    channel = request.args.get("channel")
    category = request.args.get("category")
    search = request.args.get("search")

    query = "SELECT * FROM programs WHERE 1=1"
    params = []

    if channel:
        query += " AND LOWER(channel) = LOWER(?)"
        params.append(channel.strip())
    if category:
        query += " AND LOWER(category) = LOWER(?)"
        params.append(category.strip())
    if search:
        query += " AND (LOWER(name) LIKE ? OR LOWER(description) LIKE ?)"
        like_term = f"%{search.strip().lower()}%"
        params.extend([like_term, like_term])

    query += " ORDER BY name ASC"
    rows = conn.execute(query, params).fetchall()
    return jsonify(rows_to_dict_list(rows)), 200


@program_bp.route("/api/programs/<program_id>", methods=["GET"])
def get_program(program_id):
    """Returns a single program by ID."""
    conn = get_db()
    row = conn.execute("SELECT * FROM programs WHERE id = ?", (program_id,)).fetchone()
    if not row:
        return jsonify({"error": f"Program with id '{program_id}' not found"}), 404
    return jsonify(row_to_dict(row)), 200


@program_bp.route("/api/programs", methods=["POST"])
def create_program():
    """Creates a new program with server-side validation."""
    data = request.get_json(silent=True)
    cleaned, err = validate_program_data(data, is_update=False)
    if err:
        return jsonify({"error": err}), 400

    program_id = data.get("id") or f"prog-{uuid.uuid4().hex[:6]}"
    conn = get_db()

    # Check for duplicate ID
    existing = conn.execute("SELECT id FROM programs WHERE id = ?", (program_id,)).fetchone()
    if existing:
        return jsonify({"error": f"Program with id '{program_id}' already exists"}), 409

    conn.execute(
        """
        INSERT INTO programs (id, name, channel, category, language, description, duration, video_status, audio_status, compliance_score)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (
            program_id,
            cleaned["name"],
            cleaned["channel"],
            cleaned["category"],
            cleaned.get("language", "English"),
            cleaned.get("description", ""),
            cleaned["duration"],
            cleaned.get("video_status", "Optimal"),
            cleaned.get("audio_status", "Optimal"),
            cleaned.get("compliance_score", 98)
        )
    )
    conn.commit()

    created = conn.execute("SELECT * FROM programs WHERE id = ?", (program_id,)).fetchone()
    return jsonify(row_to_dict(created)), 201


@program_bp.route("/api/programs/<program_id>", methods=["PUT"])
def update_program(program_id):
    """Updates an existing program."""
    conn = get_db()
    existing = conn.execute("SELECT * FROM programs WHERE id = ?", (program_id,)).fetchone()
    if not existing:
        return jsonify({"error": f"Program with id '{program_id}' not found"}), 404

    data = request.get_json(silent=True)
    cleaned, err = validate_program_data(data, is_update=True)
    if err:
        return jsonify({"error": err}), 400

    if not cleaned:
        return jsonify({"error": "No valid fields provided to update"}), 400

    fields = []
    values = []
    for k, v in cleaned.items():
        fields.append(f"{k} = ?")
        values.append(v)

    fields.append("updated_at = datetime('now')")
    values.append(program_id)

    query = f"UPDATE programs SET {', '.join(fields)} WHERE id = ?"
    conn.execute(query, values)
    conn.commit()

    updated = conn.execute("SELECT * FROM programs WHERE id = ?", (program_id,)).fetchone()
    return jsonify(row_to_dict(updated)), 200


@program_bp.route("/api/programs/<program_id>", methods=["DELETE"])
def delete_program(program_id):
    """Deletes a program and cascades to its schedules."""
    conn = get_db()
    existing = conn.execute("SELECT id FROM programs WHERE id = ?", (program_id,)).fetchone()
    if not existing:
        return jsonify({"error": f"Program with id '{program_id}' not found"}), 404

    conn.execute("DELETE FROM programs WHERE id = ?", (program_id,))
    conn.commit()
    return jsonify({"message": f"Program '{program_id}' successfully deleted"}), 200
