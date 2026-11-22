import os
import tempfile
import unittest
import sys

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from app import create_app
from database import init_db, get_db


class SchedulesApiTestCase(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory(ignore_cleanup_errors=True)
        self.db_path = os.path.join(self.temp_dir.name, "test_schedules.db")
        init_db(self.db_path)

        self.app = create_app({"TESTING": True, "DATABASE": self.db_path})
        self.client = self.app.test_client()

        # Seed two base programs
        self.client.post("/api/programs", json={
            "id": "prog-news",
            "name": "Morning News",
            "channel": "DD National",
            "category": "News",
            "duration": 60
        })
        self.client.post("/api/programs", json={
            "id": "prog-sports",
            "name": "Live Cricket",
            "channel": "DD Sports HD",
            "category": "Sports",
            "duration": 180
        })

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_create_schedule_valid(self):
        """POST /api/schedules creates valid schedule slot."""
        payload = {
            "id": "sch-1",
            "program_id": "prog-news",
            "channel": "DD National",
            "broadcast_date": "2026-10-15",
            "start_time": "10:00",
            "end_time": "11:00",
            "status": "Scheduled"
        }
        res = self.client.post("/api/schedules", json=payload)
        self.assertEqual(res.status_code, 201)
        data = res.get_json()
        self.assertEqual(data["id"], "sch-1")
        self.assertEqual(data["channel"], "DD National")
        self.assertEqual(data["program_title"], "Morning News")

    def test_create_schedule_invalid_chronology(self):
        """POST /api/schedules rejects end_time <= start_time with 400."""
        # end_time earlier than start_time
        payload = {
            "program_id": "prog-news",
            "channel": "DD National",
            "broadcast_date": "2026-10-15",
            "start_time": "11:00",
            "end_time": "10:00"
        }
        res = self.client.post("/api/schedules", json=payload)
        self.assertEqual(res.status_code, 400)
        self.assertIn("positive duration", res.get_json()["error"])

        # equal start and end time (zero duration)
        payload["end_time"] = "11:00"
        res_zero = self.client.post("/api/schedules", json=payload)
        self.assertEqual(res_zero.status_code, 400)

    def test_overlap_conflict_returns_409(self):
        """
        POST /api/schedules detects overlapping schedules on the same channel and date.
        Condition: start1 < end2 AND start2 < end1 returns HTTP 409.
        """
        # Create base schedule: 10:00 to 11:00
        self.client.post("/api/schedules", json={
            "id": "sch-base",
            "program_id": "prog-news",
            "channel": "DD National",
            "broadcast_date": "2026-10-15",
            "start_time": "10:00",
            "end_time": "11:00"
        })

        # Overlapping slot: 10:30 to 11:30 (start1=10:00 < end2=11:30 and start2=10:30 < end1=11:00)
        res_overlap = self.client.post("/api/schedules", json={
            "id": "sch-conflict",
            "program_id": "prog-news",
            "channel": "DD National",
            "broadcast_date": "2026-10-15",
            "start_time": "10:30",
            "end_time": "11:30"
        })
        self.assertEqual(res_overlap.status_code, 409)
        data = res_overlap.get_json()
        self.assertIn("Conflicting broadcast schedule detected", data["error"])
        self.assertEqual(data["conflicting_schedule"]["id"], "sch-base")

    def test_back_to_back_broadcast_allowed(self):
        """
        Back-to-back broadcasts (one ends exactly when another starts)
        must NOT trigger an overlap and must succeed (HTTP 201).
        """
        # Schedule 1: 10:00 to 11:00
        res1 = self.client.post("/api/schedules", json={
            "id": "sch-slot1",
            "program_id": "prog-news",
            "channel": "DD National",
            "broadcast_date": "2026-10-15",
            "start_time": "10:00",
            "end_time": "11:00"
        })
        self.assertEqual(res1.status_code, 201)

        # Schedule 2: 11:00 to 12:00 (starts exactly when slot 1 ends)
        res2 = self.client.post("/api/schedules", json={
            "id": "sch-slot2",
            "program_id": "prog-news",
            "channel": "DD National",
            "broadcast_date": "2026-10-15",
            "start_time": "11:00",
            "end_time": "12:00"
        })
        self.assertEqual(res2.status_code, 201)

    def test_same_time_different_channel_allowed(self):
        """Same time slot on different channels must NOT conflict."""
        res1 = self.client.post("/api/schedules", json={
            "program_id": "prog-news",
            "channel": "DD National",
            "broadcast_date": "2026-10-15",
            "start_time": "14:00",
            "end_time": "16:00"
        })
        self.assertEqual(res1.status_code, 201)

        res2 = self.client.post("/api/schedules", json={
            "program_id": "prog-sports",
            "channel": "DD Sports HD",
            "broadcast_date": "2026-10-15",
            "start_time": "14:00",
            "end_time": "16:00"
        })
        self.assertEqual(res2.status_code, 201)

    def test_same_time_different_date_allowed(self):
        """Same time slot on different dates must NOT conflict."""
        res1 = self.client.post("/api/schedules", json={
            "program_id": "prog-news",
            "channel": "DD National",
            "broadcast_date": "2026-10-15",
            "start_time": "18:00",
            "end_time": "19:00"
        })
        self.assertEqual(res1.status_code, 201)

        res2 = self.client.post("/api/schedules", json={
            "program_id": "prog-news",
            "channel": "DD National",
            "broadcast_date": "2026-10-16",
            "start_time": "18:00",
            "end_time": "19:00"
        })
        self.assertEqual(res2.status_code, 201)

    def test_update_schedule_self_exclusion(self):
        """Updating a schedule must not conflict with itself."""
        self.client.post("/api/schedules", json={
            "id": "sch-self",
            "program_id": "prog-news",
            "channel": "DD National",
            "broadcast_date": "2026-10-15",
            "start_time": "08:00",
            "end_time": "09:00",
            "remarks": "Original note"
        })

        # Update note without changing time
        update_res = self.client.put("/api/schedules/sch-self", json={
            "remarks": "Updated note",
            "status": "Live"
        })
        self.assertEqual(update_res.status_code, 200)
        self.assertEqual(update_res.get_json()["status"], "Live")

    def test_status_log_recorded_on_status_change(self):
        """Status change logs historical entry into status_log table."""
        self.client.post("/api/schedules", json={
            "id": "sch-logged",
            "program_id": "prog-news",
            "channel": "DD National",
            "broadcast_date": "2026-10-15",
            "start_time": "07:00",
            "end_time": "08:00",
            "status": "Scheduled"
        })

        self.client.put("/api/schedules/sch-logged", json={"status": "Live"})
        self.client.put("/api/schedules/sch-logged", json={"status": "Completed"})

        conn = get_db(self.db_path)
        try:
            logs = conn.execute("SELECT * FROM status_log WHERE schedule_id = 'sch-logged'").fetchall()
            # Initial create + 2 updates = 3 log entries
            self.assertEqual(len(logs), 3)
        finally:
            conn.close()


if __name__ == "__main__":
    unittest.main()
