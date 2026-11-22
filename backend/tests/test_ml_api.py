"""
Digital Broadcast Monitoring - ML API and Route Preservation Unit Tests

Validates:
1. GET /api/ml/health endpoint.
2. POST /api/ml/stalling-risk with valid inputs.
3. 400 error handling on missing or malformed inputs and unknown categorical values.
4. 503 error handling when the ML model is unavailable (no silent mock returns).
5. Unbroken preservation of all existing API endpoints.
"""

import os
import sys
import unittest
from unittest.mock import patch

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from app import create_app
from database import init_db


class MLAPITestCase(unittest.TestCase):

    def setUp(self):
        self.app = create_app({"TESTING": True})
        self.client = self.app.test_client()

    def test_ml_health_endpoint(self):
        """GET /api/ml/health returns model readiness and schema."""
        res = self.client.get("/api/ml/health")
        self.assertEqual(res.status_code, 200)
        data = res.get_json()

        self.assertEqual(data["status"], "READY")
        self.assertTrue(data["is_ready"])
        self.assertEqual(data["model_name"], "EarlySessionStallingClassifier")
        self.assertTrue(data["model_version"].startswith("1."))
        self.assertEqual(len(data["expected_input_fields"]), 7)
        self.assertIn("provenance", data)

    def test_ml_stalling_risk_successful_prediction(self):
        """POST /api/ml/stalling-risk returns structured model prediction."""
        payload = {
            "year": "2020",
            "location": "Zagreb",
            "bandwidth": "unlimited",
            "adblock": "1",
            "dl_throughput_first1s": 3500000.0,
            "dl_throughput_first5s": 4200000.0,
            "dl_throughput_first10s": 4800000.0,
        }
        res = self.client.post("/api/ml/stalling-risk", json=payload)
        self.assertEqual(res.status_code, 200)
        data = res.get_json()

        self.assertIn("predicted_class", data)
        self.assertIn(data["predicted_class"], [0, 1])
        self.assertIn("prediction_label", data)
        self.assertIn(data["prediction_label"], ["no_stalling", "stalling"])
        self.assertIn("stalling_risk_score", data)
        self.assertGreaterEqual(data["stalling_risk_score"], 0.0)
        self.assertLessEqual(data["stalling_risk_score"], 1.0)
        self.assertEqual(data["model_name"], "EarlySessionStallingClassifier")
        self.assertIn("provenance", data)
        self.assertIn("disclaimer", data["provenance"])

    def test_ml_stalling_risk_missing_fields_rejection(self):
        """POST /api/ml/stalling-risk rejects payload when required fields are missing."""
        payload = {
            "year": "2020",
            "location": "Zagreb",
            # bandwidth is missing
            "adblock": "1",
            "dl_throughput_first1s": 3500000.0,
            "dl_throughput_first5s": 4200000.0,
            "dl_throughput_first10s": 4800000.0,
        }
        res = self.client.post("/api/ml/stalling-risk", json=payload)
        self.assertEqual(res.status_code, 400)
        data = res.get_json()
        self.assertIn("error", data)
        self.assertIn("Missing required input features", data["details"])

    def test_ml_stalling_risk_unknown_categorical_rejection(self):
        """POST /api/ml/stalling-risk rejects unknown categorical values with 400."""
        payload = {
            "year": "2020",
            "location": "Tokyo",  # Not in allowed locations
            "bandwidth": "unlimited",
            "adblock": "1",
            "dl_throughput_first1s": 3500000.0,
            "dl_throughput_first5s": 4200000.0,
            "dl_throughput_first10s": 4800000.0,
        }
        res = self.client.post("/api/ml/stalling-risk", json=payload)
        self.assertEqual(res.status_code, 400)
        data = res.get_json()
        self.assertIn("Invalid value for 'location'", data["details"])

    def test_ml_stalling_risk_malformed_numeric_rejection(self):
        """POST /api/ml/stalling-risk rejects non-numeric or negative throughput with 400."""
        payload = {
            "year": "2020",
            "location": "Zagreb",
            "bandwidth": "unlimited",
            "adblock": "1",
            "dl_throughput_first1s": "invalid_string",
            "dl_throughput_first5s": 4200000.0,
            "dl_throughput_first10s": 4800000.0,
        }
        res = self.client.post("/api/ml/stalling-risk", json=payload)
        self.assertEqual(res.status_code, 400)

    def test_ml_model_unavailable_handling(self):
        """POST /api/ml/stalling-risk returns 503 when model cannot be loaded."""
        valid_payload = {
            "year": "2020",
            "location": "Zagreb",
            "bandwidth": "unlimited",
            "adblock": "1",
            "dl_throughput_first1s": 3500000.0,
            "dl_throughput_first5s": 4200000.0,
            "dl_throughput_first10s": 4800000.0,
        }
        with patch("services.stalling_ml_service.load_stalling_pipeline", return_value=(None, None, "Model file missing")):
            res = self.client.post("/api/ml/stalling-risk", json=valid_payload)
            self.assertEqual(res.status_code, 503)
            data = res.get_json()
            self.assertEqual(data["error"], "Inference error")
            self.assertIn("Model file missing", data["details"])

    def test_existing_backend_routes_preserved(self):
        """Verifies that all pre-existing REST routes remain functional and unmodified."""
        # 1. Health
        res = self.client.get("/api/health")
        self.assertEqual(res.status_code, 200)

        # 2. Programs
        res = self.client.get("/api/programs")
        self.assertEqual(res.status_code, 200)

        # 3. Schedules
        res = self.client.get("/api/schedules")
        self.assertEqual(res.status_code, 200)

        # 4. Dashboard
        res = self.client.get("/api/dashboard")
        self.assertEqual(res.status_code, 200)

        # 5. Reports
        res = self.client.get("/api/reports")
        self.assertEqual(res.status_code, 200)

        # 6. Alerts
        res = self.client.get("/api/alerts")
        self.assertEqual(res.status_code, 200)


if __name__ == "__main__":
    unittest.main()
