import os
import tempfile
import unittest
import sys

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from app import create_app
from database import init_db


class ProgramsApiTestCase(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory(ignore_cleanup_errors=True)
        self.db_path = os.path.join(self.temp_dir.name, "test_programs.db")
        init_db(self.db_path)

        self.app = create_app({"TESTING": True, "DATABASE": self.db_path})
        self.client = self.app.test_client()

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_get_programs_empty(self):
        """GET /api/programs returns empty list on clean database."""
        res = self.client.get("/api/programs")
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.get_json(), [])

    def test_create_program_valid(self):
        """POST /api/programs creates program with positive duration."""
        payload = {
            "name": "Evening Prime",
            "channel": "DD National",
            "category": "News",
            "language": "Hindi",
            "duration": 60,
            "description": "Daily prime news"
        }
        res = self.client.post("/api/programs", json=payload)
        self.assertEqual(res.status_code, 201)
        data = res.get_json()
        self.assertEqual(data["name"], "Evening Prime")
        self.assertEqual(data["channel"], "DD National")
        self.assertEqual(data["duration"], 60)

    def test_create_program_missing_fields(self):
        """POST /api/programs rejects missing required fields with 400."""
        # Missing channel & category
        res = self.client.post("/api/programs", json={"name": "Bad Program", "duration": 30})
        self.assertEqual(res.status_code, 400)
        self.assertIn("error", res.get_json())

    def test_create_program_invalid_duration(self):
        """POST /api/programs rejects non-positive duration with 400."""
        payload = {
            "name": "Invalid Duration Prog",
            "channel": "DD National",
            "category": "News",
            "duration": -10
        }
        res = self.client.post("/api/programs", json=payload)
        self.assertEqual(res.status_code, 400)
        self.assertIn("positive integer", res.get_json()["error"])

    def test_get_program_by_id(self):
        """GET /api/programs/<id> returns 200 if found or 404 if missing."""
        create_res = self.client.post("/api/programs", json={
            "id": "prog-test-1",
            "name": "Target Prog",
            "channel": "DD India",
            "category": "Talk Show",
            "duration": 45
        })
        self.assertEqual(create_res.status_code, 201)

        get_res = self.client.get("/api/programs/prog-test-1")
        self.assertEqual(get_res.status_code, 200)
        self.assertEqual(get_res.get_json()["name"], "Target Prog")

        # 404 check
        missing_res = self.client.get("/api/programs/non-existent-id")
        self.assertEqual(missing_res.status_code, 404)

    def test_update_program(self):
        """PUT /api/programs/<id> updates existing program."""
        self.client.post("/api/programs", json={
            "id": "prog-edit",
            "name": "Old Title",
            "channel": "DD Sports HD",
            "category": "Sports",
            "duration": 90
        })

        update_res = self.client.put("/api/programs/prog-edit", json={
            "name": "New Championship Title",
            "duration": 120
        })
        self.assertEqual(update_res.status_code, 200)
        updated_data = update_res.get_json()
        self.assertEqual(updated_data["name"], "New Championship Title")
        self.assertEqual(updated_data["duration"], 120)

    def test_delete_program(self):
        """DELETE /api/programs/<id> deletes program."""
        self.client.post("/api/programs", json={
            "id": "prog-del",
            "name": "To Delete",
            "channel": "DD Bharati",
            "category": "Documentary",
            "duration": 30
        })

        del_res = self.client.delete("/api/programs/prog-del")
        self.assertEqual(del_res.status_code, 200)

        get_res = self.client.get("/api/programs/prog-del")
        self.assertEqual(get_res.status_code, 404)


if __name__ == "__main__":
    unittest.main()
