import os
import tempfile
import unittest
import sys

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from app import create_app
from database import init_db
from services.ml_service import (
    extract_schedule_features,
    run_inference,
    load_model,
    MODEL_PATH
)


class MLServiceTestCase(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory(ignore_cleanup_errors=True)
        self.db_path = os.path.join(self.temp_dir.name, "test_ml.db")
        init_db(self.db_path)

        self.app = create_app({"TESTING": True, "DATABASE": self.db_path})
        self.client = self.app.test_client()

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_feature_extraction(self):
        """Feature extractor produces 1x7 numpy vector with numeric features."""
        feat = extract_schedule_features(
            duration_minutes=60,
            start_time_str="20:00",
            end_time_str="21:00",
            category_str="News"
        )
        self.assertEqual(feat.shape, (1, 7))
        # duration
        self.assertEqual(feat[0][0], 60.0)
        # start_hour
        self.assertEqual(feat[0][1], 20.0)
        # start_minute
        self.assertEqual(feat[0][2], 0.0)
        # end_hour
        self.assertEqual(feat[0][3], 21.0)
        # is_prime_time (20:00 is prime time)
        self.assertEqual(feat[0][4], 1.0)
        # is_overnight
        self.assertEqual(feat[0][5], 0.0)
        # category_code for News is 1
        self.assertEqual(feat[0][6], 1.0)

    def test_trained_model_inference_output(self):
        """Inference with trained model outputs valid anomaly score and latency."""
        self.assertTrue(os.path.exists(MODEL_PATH), "Trained model file should exist")
        res = run_inference(
            duration_minutes=30,
            start_time="10:00",
            end_time="10:30",
            category="News",
            channel="DD National"
        )
        self.assertTrue(res["model_loaded"])
        self.assertEqual(res["model_status"], "READY")
        self.assertIsInstance(res["is_anomaly"], bool)
        self.assertGreaterEqual(res["anomaly_score"], 0.0)
        self.assertLessEqual(res["anomaly_score"], 1.0)
        self.assertGreater(res["latency_ms"], 0.0)
        self.assertIn("compliance_prediction", res)

    def test_schedule_analysis_endpoint(self):
        """GET /api/schedules/<id>/analysis combines rule validation and ML inference."""
        # Create program and schedule
        self.client.post("/api/programs", json={
            "id": "p-analysis", "name": "Documentary Special", "channel": "DD India",
            "category": "Documentary", "duration": 45
        })
        self.client.post("/api/schedules", json={
            "id": "s-analysis", "program_id": "p-analysis", "channel": "DD India",
            "broadcast_date": "2026-10-15", "start_time": "12:00", "end_time": "12:45"
        })

        res = self.client.get("/api/schedules/s-analysis/analysis")
        self.assertEqual(res.status_code, 200)
        data = res.get_json()

        # Rule validation
        self.assertIn("rule_validation", data)
        self.assertTrue(data["rule_validation"]["is_valid"])
        self.assertEqual(data["rule_validation"]["status"], "PASS")

        # ML inference
        self.assertIn("ml_analysis", data)
        self.assertTrue(data["ml_analysis"]["model_loaded"])
        self.assertIsInstance(data["ml_analysis"]["anomaly_score"], (int, float))


if __name__ == "__main__":
    unittest.main()
