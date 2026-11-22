"""
Digital Broadcast Monitoring - Database Seeder
Populates initial database records matching the broadcast monitoring baseline:
- 24 standardized programs
- 8 initial schedule slots for today
- Baseline alerts
- Initial status logs
"""

import os
import sys
from datetime import datetime, date

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from database import get_db, init_db, DEFAULT_DB_PATH

PROGRAMS_SEED = [
    {
        "id": "prog-001",
        "name": "Morning News",
        "channel": "DD National",
        "category": "News",
        "language": "English / Hindi",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 99,
        "duration": 30,
        "description": "Flagship early morning national news bulletin and headline summary."
    },
    {
        "id": "prog-002",
        "name": "Educational Hour",
        "channel": "DD Bharati",
        "category": "Talk Show",
        "language": "Hindi / English",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 97,
        "duration": 60,
        "description": "Interactive STEM and humanities lecture broadcast for secondary education."
    },
    {
        "id": "prog-003",
        "name": "Documentary Special",
        "channel": "DD India",
        "category": "Documentary",
        "language": "English",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 95,
        "duration": 45,
        "description": "Exploration of cultural heritage, archaeological marvels and environmental conservation."
    },
    {
        "id": "prog-004",
        "name": "National Prime News Hour",
        "channel": "DD National HD",
        "category": "News",
        "language": "English / Hindi",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 98,
        "duration": 60,
        "description": "Daily flagship evening bulletin covering national and international affairs."
    },
    {
        "id": "prog-005",
        "name": "Cricket Championship Live",
        "channel": "DD Sports HD",
        "category": "Sports",
        "language": "Hindi",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 94,
        "duration": 180,
        "description": "Live telecast of the regional championship semi-finals with multi-audio feeds."
    },
    {
        "id": "prog-006",
        "name": "Heritage of India",
        "channel": "DD Bharati",
        "category": "Documentary",
        "language": "English",
        "video_status": "Degraded",
        "audio_status": "Optimal",
        "compliance_score": 88,
        "duration": 45,
        "description": "Architectural survey of classical temples and medieval forts."
    },
    {
        "id": "prog-007",
        "name": "Morning Financial Express",
        "channel": "DD India",
        "category": "Talk Show",
        "language": "English",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 97,
        "duration": 30,
        "description": "Market opening analysis and interviews with central trade analysts."
    },
    {
        "id": "prog-008",
        "name": "Folk Melodies & Classical Dance",
        "channel": "DD Retro",
        "category": "Entertainment",
        "language": "Hindi",
        "video_status": "Optimal",
        "audio_status": "Critical",
        "compliance_score": 82,
        "duration": 90,
        "description": "Archival broadcasts of classical concerts and traditional folk recitals."
    },
    {
        "id": "prog-009",
        "name": "Public Service: Health Awareness",
        "channel": "DD National",
        "category": "Commercial",
        "language": "Multilingual",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 100,
        "duration": 15,
        "description": "Government sponsored public awareness announcement block."
    },
    {
        "id": "prog-010",
        "name": "Agricultural Horizons",
        "channel": "DD Kisan",
        "category": "Talk Show",
        "language": "Hindi",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 96,
        "duration": 45,
        "description": "Farmer advisories and rural economy discussions."
    },
    {
        "id": "prog-011",
        "name": "Parliamentary Proceedings Live",
        "channel": "Sansad TV",
        "category": "News",
        "language": "Hindi / English",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 99,
        "duration": 120,
        "description": "Live parliamentary floor debates."
    },
    {
        "id": "prog-012",
        "name": "Science & Tech Frontiers",
        "channel": "DD India",
        "category": "Documentary",
        "language": "English",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 97,
        "duration": 30,
        "description": "Innovations in AI, rocketry and green energy."
    },
    {
        "id": "prog-013",
        "name": "Youth & Culture Pulse",
        "channel": "DD Bharati",
        "category": "Entertainment",
        "language": "Hindi",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 94,
        "duration": 60,
        "description": "Spotlight on contemporary theatre and creative arts."
    },
    {
        "id": "prog-014",
        "name": "Badminton Super League",
        "channel": "DD Sports HD",
        "category": "Sports",
        "language": "Hindi / English",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 96,
        "duration": 90,
        "description": "National badminton tournament matches."
    },
    {
        "id": "prog-015",
        "name": "Sanskrit Varta & Bulletin",
        "channel": "DD National",
        "category": "News",
        "language": "Sanskrit",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 98,
        "duration": 15,
        "description": "Daily Sanskrit language news telecast."
    },
    {
        "id": "prog-016",
        "name": "Wildlife Sanctuaries: Western Ghats",
        "channel": "DD Bharati",
        "category": "Documentary",
        "language": "English",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 99,
        "duration": 45,
        "description": "Biodiversity hotspots documentation."
    },
    {
        "id": "prog-017",
        "name": "Consumer Rights Watch",
        "channel": "DD National",
        "category": "Talk Show",
        "language": "Hindi",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 95,
        "duration": 30,
        "description": "Legal protections for retail consumers."
    },
    {
        "id": "prog-018",
        "name": "Traditional Yoga for Wellness",
        "channel": "DD India",
        "category": "Talk Show",
        "language": "English",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 98,
        "duration": 30,
        "description": "Morning wellness instruction and asanas."
    },
    {
        "id": "prog-019",
        "name": "Classical Sitar & Tabla Duet",
        "channel": "DD Retro",
        "category": "Entertainment",
        "language": "Hindi",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 97,
        "duration": 60,
        "description": "Maestros in concert archives."
    },
    {
        "id": "prog-020",
        "name": "Global Trade Digest",
        "channel": "DD India",
        "category": "Talk Show",
        "language": "English",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 96,
        "duration": 30,
        "description": "Import/export trends and macroeconomic indicators."
    },
    {
        "id": "prog-021",
        "name": "Women Champions in Sports",
        "channel": "DD Sports HD",
        "category": "Sports",
        "language": "Hindi",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 98,
        "duration": 45,
        "description": "Profile interviews with Olympic medalists."
    },
    {
        "id": "prog-022",
        "name": "Road Safety Initiative",
        "channel": "DD National",
        "category": "Commercial",
        "language": "Multilingual",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 100,
        "duration": 15,
        "description": "Public advocacy campaign on highway safety."
    },
    {
        "id": "prog-023",
        "name": "Literature & Philosophy Forum",
        "channel": "DD Bharati",
        "category": "Talk Show",
        "language": "Hindi",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 93,
        "duration": 60,
        "description": "Discussion with eminent authors and essayists."
    },
    {
        "id": "prog-024",
        "name": "Midday Speed News",
        "channel": "DD National HD",
        "category": "News",
        "language": "Hindi / English",
        "video_status": "Optimal",
        "audio_status": "Optimal",
        "compliance_score": 99,
        "duration": 30,
        "description": "Rapid 50 top stories bulletin."
    }
]


def seed_database(db_path=None):
    """Initializes tables and seeds the database with initial baseline data."""
    target_path = db_path or DEFAULT_DB_PATH
    init_db(target_path)
    conn = get_db(target_path)

    today_str = date.today().isoformat()

    # 1. Seed Programs
    for p in PROGRAMS_SEED:
        conn.execute(
            """
            INSERT OR REPLACE INTO programs
            (id, name, channel, category, language, description, duration, video_status, audio_status, compliance_score)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                p["id"], p["name"], p["channel"], p["category"], p["language"],
                p["description"], p["duration"], p["video_status"], p["audio_status"],
                p["compliance_score"]
            )
        )

    # 2. Seed Schedules for today
    schedules_seed = [
        ("sch-101", "prog-001", "DD National", today_str, "10:00", "10:30", "Completed", "Morning news bulletin completed on schedule.", 1, 0),
        ("sch-102", "prog-002", "DD Bharati", today_str, "11:00", "12:00", "Live", "On-air stream live. Video and audio nominal.", 1, 0),
        ("sch-103", "prog-003", "DD India", today_str, "12:00", "12:45", "Scheduled", "Scheduled transmission ingest ready.", 1, 0),
        ("sch-104", "prog-005", "DD Sports HD", today_str, "14:00", "17:00", "Scheduled", "Multi-camera stadium feed.", 1, 0),
        ("sch-105", "prog-004", "DD National HD", today_str, "20:00", "21:00", "Scheduled", "Evening prime bulletin.", 1, 0),
        ("sch-106", "prog-009", "DD National", today_str, "21:00", "21:15", "Scheduled", "Statutory public health message.", 1, 0),
        ("sch-107", "prog-008", "DD Retro", today_str, "21:30", "23:00", "Scheduled", "Archival music programming.", 0, 0),
        ("sch-108", "prog-006", "DD Bharati", today_str, "23:15", "23:45", "Scheduled", "Late night cultural broadcast.", 1, 0)
    ]

    for s in schedules_seed:
        conn.execute(
            """
            INSERT OR REPLACE INTO schedules
            (id, program_id, channel, broadcast_date, start_time, end_time, status, remarks, is_verified, has_overlap)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            s
        )

    # 3. Seed Alerts
    alerts_seed = [
        (
            "alt-01",
            "11:45:00 AM IST",
            "Schedule Overlap Detected",
            "Conflicting broadcast intervals detected: Educational Hour overlaps with Special Bulletin on DD National mux feed.",
            "ANOMALY",
            "DD National",
            "Educational Hour",
            "sch-102",
            0
        ),
        (
            "alt-02",
            "10:35:12 AM IST",
            "Unusual Duration Alert",
            "Transmission elapsed duration exceeded programmed schedule slot limit by 15.4 minutes.",
            "WARNING",
            "DD Sports HD",
            "Morning News",
            "sch-101",
            0
        )
    ]

    for a in alerts_seed:
        conn.execute(
            """
            INSERT OR REPLACE INTO alerts
            (id, timestamp, title, description, severity, channel, program_title, schedule_id, is_simulated)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            a
        )

    # 4. Seed Status Log
    status_logs_seed = [
        ("sch-101", "Live", "Completed", "Broadcast completed successfully"),
        ("sch-102", "Scheduled", "Live", "On-air stream went live"),
        ("sch-103", None, "Scheduled", "Slot created"),
        ("sch-104", None, "Scheduled", "Slot created"),
        ("sch-105", None, "Scheduled", "Slot created"),
        ("sch-106", None, "Scheduled", "Slot created"),
        ("sch-107", None, "Scheduled", "Slot created"),
        ("sch-108", None, "Scheduled", "Slot created")
    ]

    for sl in status_logs_seed:
        existing_log = conn.execute(
            "SELECT id FROM status_log WHERE schedule_id = ? AND new_status = ?",
            (sl[0], sl[2])
        ).fetchone()
        if not existing_log:
            conn.execute(
                """
                INSERT INTO status_log (schedule_id, old_status, new_status, remarks)
                VALUES (?, ?, ?, ?)
                """,
                sl
            )

    conn.commit()
    print(f"Successfully seeded database at {target_path} with {len(PROGRAMS_SEED)} programs and {len(schedules_seed)} schedules.")


if __name__ == "__main__":
    seed_database()
