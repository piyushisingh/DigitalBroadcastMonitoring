import os
import tempfile
import unittest
import sys

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from app import create_app
from database import init_db


class AlertsAndHealthTestCase(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory(ignore_cleanup_errors=True)
        self.db_path = os.path.join(self.temp_dir.name, "test_alerts.db")
        init_db(self.db_path)

        self.app = create_app({"TESTING": True, "DATABASE": self.db_path})
        self.client = self.app.test_client()

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_health_check(self):
        """GET /api/health returns healthy database and ready model."""
        res = self.client.get("/api/health")
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertEqual(data["status"], "ok")
        self.assertEqual(data["database"], "healthy")
        self.assertIn(data["ml_model"], ["ready", "not_trained"])

    def test_alert_crud_lifecycle(self):
        """POST /api/alerts, GET /api/alerts, and DELETE /api/alerts/<id>."""
        # Create alert
        create_res = self.client.post("/api/alerts", json={
            "id": "alt-test-1",
            "title": "Audio Distortion Alert",
            "description": "Audio clipping detected on channel feed",
            "severity": "WARNING",
            "channel": "DD Retro"
        })
        self.assertEqual(create_res.status_code, 201)
        self.assertEqual(create_res.get_json()["id"], "alt-test-1")

        # Query alerts
        get_res = self.client.get("/api/alerts?severity=WARNING")
        self.assertEqual(get_res.status_code, 200)
        alerts = get_res.get_json()
        self.assertEqual(len(alerts), 1)
        self.assertEqual(alerts[0]["title"], "Audio Distortion Alert")

        # Dismiss alert
        del_res = self.client.delete("/api/alerts/alt-test-1")
        self.assertEqual(del_res.status_code, 200)

        # Query again - should be empty
        empty_res = self.client.get("/api/alerts")
        self.assertEqual(empty_res.status_code, 200)
        self.assertEqual(len(empty_res.get_json()), 0)


if __name__ == "__main__":
    unittest.main()
