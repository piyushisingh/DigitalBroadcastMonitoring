import os
import tempfile
import unittest
import sys

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from app import create_app
from database import init_db


class DashboardAndReportsTestCase(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory(ignore_cleanup_errors=True)
        self.db_path = os.path.join(self.temp_dir.name, "test_dash_rep.db")
        init_db(self.db_path)

        self.app = create_app({"TESTING": True, "DATABASE": self.db_path})
        self.client = self.app.test_client()

        # Seed 3 programs with distinct categories
        self.client.post("/api/programs", json={
            "id": "p1", "name": "Morning News", "channel": "DD National",
            "category": "News", "duration": 30, "compliance_score": 98
        })
        self.client.post("/api/programs", json={
            "id": "p2", "name": "Cricket Live", "channel": "DD Sports HD",
            "category": "Sports", "duration": 120, "compliance_score": 92
        })
        self.client.post("/api/programs", json={
            "id": "p3", "name": "Music Fest", "channel": "DD Retro",
            "category": "Entertainment", "duration": 60, "compliance_score": 90
        })

        # Seed 3 schedules with distinct statuses
        # 30 min completed
        self.client.post("/api/schedules", json={
            "id": "s1", "program_id": "p1", "channel": "DD National",
            "broadcast_date": "2026-10-14", "start_time": "08:00", "end_time": "08:30",
            "status": "Completed"
        })
        # 120 min live
        self.client.post("/api/schedules", json={
            "id": "s2", "program_id": "p2", "channel": "DD Sports HD",
            "broadcast_date": "2026-10-14", "start_time": "10:00", "end_time": "12:00",
            "status": "Live"
        })
        # 60 min scheduled
        self.client.post("/api/schedules", json={
            "id": "s3", "program_id": "p3", "channel": "DD Retro",
            "broadcast_date": "2026-10-14", "start_time": "14:00", "end_time": "15:00",
            "status": "Scheduled"
        })

        # Seed 1 alert
        self.client.post("/api/alerts", json={
            "title": "Bitrate Drop",
            "channel": "DD Sports HD",
            "severity": "ANOMALY",
            "description": "Video bitrate reduced by 25%"
        })

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_dashboard_metrics_from_actual_db_records(self):
        """Dashboard counts must derive strictly from database rows without mock baselines."""
        res = self.client.get("/api/dashboard")
        self.assertEqual(res.status_code, 200)
        data = res.get_json()["metrics"]

        self.assertEqual(data["total_programs"], 3)
        self.assertEqual(data["total_broadcasts"], 3)
        self.assertEqual(data["completed_count"], 1)
        self.assertEqual(data["live_count"], 1)
        self.assertEqual(data["scheduled_count"], 1)
        self.assertEqual(data["anomalies_count"], 1)
        self.assertEqual(data["active_channels"], 3)

    def test_reports_aggregation_math(self):
        """Reports total duration and distributions must calculate accurately from actual DB rows."""
        res = self.client.get("/api/reports")
        self.assertEqual(res.status_code, 200)
        body = res.get_json()
        summary = body["summary"]

        # Total minutes: 30 + 120 + 60 = 210 minutes (3 hours 30 min)
        self.assertEqual(summary["total_duration_minutes"], 210)
        self.assertEqual(summary["total_duration_hours"], 3)
        self.assertEqual(summary["total_duration_remain_minutes"], 30)

        # Category distribution
        cat_dist = body["category_distribution"]
        self.assertEqual(cat_dist.get("News"), 1)
        self.assertEqual(cat_dist.get("Sports"), 1)
        self.assertEqual(cat_dist.get("Entertainment"), 1)

        # Status distribution
        status_dist = body["status_distribution"]
        self.assertEqual(status_dist.get("Completed"), 1)
        self.assertEqual(status_dist.get("Live"), 1)
        self.assertEqual(status_dist.get("Scheduled"), 1)

    def test_deleting_schedule_dynamically_updates_dashboard(self):
        """Deleting a schedule updates dashboard metrics dynamically."""
        self.client.delete("/api/schedules/s1")
        res = self.client.get("/api/dashboard")
        metrics = res.get_json()["metrics"]
        self.assertEqual(metrics["total_broadcasts"], 2)
        self.assertEqual(metrics["completed_count"], 0)


if __name__ == "__main__":
    unittest.main()
