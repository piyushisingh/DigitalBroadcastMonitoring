import re
from datetime import datetime

VALID_SCHEDULE_STATUSES = {"Scheduled", "Live", "Completed", "Cancelled", "Rescheduled", "Flagged"}
VALID_SEVERITIES = {"ANOMALY", "WARNING", "SUCCESS"}
VALID_STREAM_STATUSES = {"Optimal", "Degraded", "Critical", "Offline"}


def parse_time_to_minutes(time_str):
    """
    Parses a time string in 'HH:MM' or 'HH:MM:SS' format into minutes from midnight.
    Returns integer minutes or None if format is invalid.
    """
    if not isinstance(time_str, str):
        return None
    time_str = time_str.strip()
    match = re.match(r"^(\d{1,2}):(\d{2})(?::\d{2})?$", time_str)
    if not match:
        return None
    hours = int(match.group(1))
    minutes = int(match.group(2))
    if hours < 0 or hours > 23 or minutes < 0 or minutes > 59:
        return None
    return hours * 60 + minutes


def validate_date(date_str):
    """
    Validates that date_str conforms to YYYY-MM-DD format.
    Returns boolean.
    """
    if not isinstance(date_str, str):
        return False
    try:
        datetime.strptime(date_str.strip(), "%Y-%m-%d")
        return True
    except ValueError:
        return False


def validate_program_data(data, is_update=False):
    """
    Validates payload for creating or updating a Program.
    Returns (cleaned_dict, error_message).
    """
    if not isinstance(data, dict):
        return None, "Request body must be a JSON object"

    name = data.get("name") or data.get("title")
    channel = data.get("channel")
    category = data.get("category")
    duration = data.get("duration") or data.get("durationMinutes")
    language = data.get("language", "English")
    description = data.get("description", "")
    video_status = data.get("video_status") or data.get("videoStatus", "Optimal")
    audio_status = data.get("audio_status") or data.get("audioStatus", "Optimal")
    compliance_score = data.get("compliance_score") or data.get("complianceScore", 98)

    errors = []

    if not is_update:
        if not name or not str(name).strip():
            errors.append("Program 'name' (or 'title') is required and cannot be empty")
        if not channel or not str(channel).strip():
            errors.append("Program 'channel' is required and cannot be empty")
        if not category or not str(category).strip():
            errors.append("Program 'category' is required and cannot be empty")
        if duration is None:
            errors.append("Program 'duration' (in minutes) is required")

    if name is not None and not str(name).strip():
        errors.append("Program 'name' cannot be blank")

    if channel is not None and not str(channel).strip():
        errors.append("Program 'channel' cannot be blank")

    if category is not None and not str(category).strip():
        errors.append("Program 'category' cannot be blank")

    if duration is not None:
        try:
            duration = int(duration)
            if duration <= 0:
                errors.append("Program duration must be a positive integer (> 0)")
        except (ValueError, TypeError):
            errors.append("Program duration must be a valid integer")

    if video_status and video_status not in VALID_STREAM_STATUSES:
        errors.append(f"Invalid video_status. Must be one of: {', '.join(sorted(VALID_STREAM_STATUSES))}")

    if audio_status and audio_status not in VALID_STREAM_STATUSES:
        errors.append(f"Invalid audio_status. Must be one of: {', '.join(sorted(VALID_STREAM_STATUSES))}")

    if compliance_score is not None:
        try:
            compliance_score = int(compliance_score)
            if compliance_score < 0 or compliance_score > 100:
                errors.append("Compliance score must be between 0 and 100")
        except (ValueError, TypeError):
            errors.append("Compliance score must be an integer")

    if errors:
        return None, "; ".join(errors)

    cleaned = {}
    if name is not None:
        cleaned["name"] = str(name).strip()
    if channel is not None:
        cleaned["channel"] = str(channel).strip()
    if category is not None:
        cleaned["category"] = str(category).strip()
    if duration is not None:
        cleaned["duration"] = duration
    if language is not None:
        cleaned["language"] = str(language).strip()
    if description is not None:
        cleaned["description"] = str(description).strip()
    if video_status is not None:
        cleaned["video_status"] = video_status
    if audio_status is not None:
        cleaned["audio_status"] = audio_status
    if compliance_score is not None:
        cleaned["compliance_score"] = compliance_score

    return cleaned, None


def validate_schedule_data(data, is_update=False):
    """
    Validates payload for creating or updating a Broadcast Schedule.
    Checks required fields, date/time format, positive duration, and valid status.
    Returns (cleaned_dict, error_message).
    """
    if not isinstance(data, dict):
        return None, "Request body must be a JSON object"

    program_id = data.get("program_id") or data.get("programId")
    channel = data.get("channel")
    broadcast_date = data.get("broadcast_date") or data.get("date")
    start_time = data.get("start_time") or data.get("startTime")
    end_time = data.get("end_time") or data.get("endTime")
    status = data.get("status", "Scheduled")
    remarks = data.get("remarks") or data.get("notes", "")
    is_verified = data.get("is_verified") if "is_verified" in data else data.get("isVerified", 1)

    errors = []

    if not is_update:
        if not program_id or not str(program_id).strip():
            errors.append("Schedule 'program_id' is required")
        if not channel or not str(channel).strip():
            errors.append("Schedule 'channel' (feed) is required")
        if not broadcast_date or not str(broadcast_date).strip():
            errors.append("Schedule 'broadcast_date' (YYYY-MM-DD) is required")
        if not start_time or not str(start_time).strip():
            errors.append("Schedule 'start_time' (HH:MM) is required")
        if not end_time or not str(end_time).strip():
            errors.append("Schedule 'end_time' (HH:MM) is required")

    if broadcast_date is not None:
        if not validate_date(broadcast_date):
            errors.append("Schedule 'broadcast_date' must be in YYYY-MM-DD format")

    start_min = None
    end_min = None

    if start_time is not None:
        start_min = parse_time_to_minutes(start_time)
        if start_min is None:
            errors.append("Schedule 'start_time' must be in valid HH:MM (24-hour) format")

    if end_time is not None:
        end_min = parse_time_to_minutes(end_time)
        if end_min is None:
            errors.append("Schedule 'end_time' must be in valid HH:MM (24-hour) format")

    # If both start and end times are available, validate duration
    if start_min is not None and end_min is not None:
        if end_min <= start_min:
            errors.append("Schedule 'end_time' must be strictly after 'start_time' (positive duration required)")

    if status is not None and status not in VALID_SCHEDULE_STATUSES:
        errors.append(f"Invalid status '{status}'. Must be one of: {', '.join(sorted(VALID_SCHEDULE_STATUSES))}")

    if errors:
        return None, "; ".join(errors)

    cleaned = {}
    if program_id is not None:
        cleaned["program_id"] = str(program_id).strip()
    if channel is not None:
        cleaned["channel"] = str(channel).strip()
    if broadcast_date is not None:
        cleaned["broadcast_date"] = str(broadcast_date).strip()
    if start_time is not None:
        # Standardize HH:MM format
        cleaned["start_time"] = f"{start_min // 60:02d}:{start_min % 60:02d}"
    if end_time is not None:
        cleaned["end_time"] = f"{end_min // 60:02d}:{end_min % 60:02d}"
    if status is not None:
        cleaned["status"] = status
    if remarks is not None:
        cleaned["remarks"] = str(remarks).strip()
    if is_verified is not None:
        cleaned["is_verified"] = 1 if bool(is_verified) else 0

    return cleaned, None


def check_schedule_conflict(channel, broadcast_date, start_time, end_time, exclude_schedule_id=None, db=None):
    """
    Checks for overlapping broadcasts on the same date and transmission channel.
    Condition: start1 < end2 AND start2 < end1.
    Back-to-back broadcasts (end1 == start2 or end2 == start1) are allowed and do NOT conflict.
    Editing a schedule excludes its own ID so it never self-conflicts.

    Returns:
        (has_conflict: bool, conflicting_schedule: dict or None)
    """
    from database import get_db, rows_to_dict_list

    conn = db or get_db()
    candidate_start = parse_time_to_minutes(start_time)
    candidate_end = parse_time_to_minutes(end_time)

    if candidate_start is None or candidate_end is None:
        return False, None

    # Fetch all existing non-cancelled schedules on the same channel and date
    query = """
        SELECT s.*, p.name as program_title
        FROM schedules s
        JOIN programs p ON s.program_id = p.id
        WHERE LOWER(s.channel) = LOWER(?)
          AND s.broadcast_date = ?
          AND s.status != 'Cancelled'
    """
    params = [channel.strip(), broadcast_date.strip()]

    if exclude_schedule_id:
        query += " AND s.id != ?"
        params.append(exclude_schedule_id)

    rows = conn.execute(query, params).fetchall()
    existing_schedules = rows_to_dict_list(rows)

    for other in existing_schedules:
        other_start = parse_time_to_minutes(other["start_time"])
        other_end = parse_time_to_minutes(other["end_time"])

        if other_start is None or other_end is None:
            continue

        # Condition: start1 < end2 AND start2 < end1
        if candidate_start < other_end and other_start < candidate_end:
            return True, other

    return False, None
