import os
import sqlite3
import tempfile
import unittest
import sys

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from database import init_db, get_db


class DatabaseTestCase(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory(ignore_cleanup_errors=True)
        self.db_path = os.path.join(self.temp_dir.name, "test_broadcast.db")
        init_db(self.db_path)
        self.conn = get_db(self.db_path)

    def tearDown(self):
        if hasattr(self, "conn") and self.conn:
            self.conn.close()
        self.temp_dir.cleanup()

    def test_schema_creates_all_tables(self):
        """Verifies that programs, schedules, alerts, and status_log tables exist."""
        cursor = self.conn.cursor()
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = {row["name"] for row in cursor.fetchall()}
        self.assertIn("programs", tables)
        self.assertIn("schedules", tables)
        self.assertIn("alerts", tables)
        self.assertIn("status_log", tables)

    def test_foreign_key_enforcement(self):
        """Foreign key constraint must prevent inserting schedule for non-existent program."""
        with self.assertRaises(sqlite3.IntegrityError):
            self.conn.execute(
                """
                INSERT INTO schedules (id, program_id, channel, broadcast_date, start_time, end_time, status)
                VALUES ('sch-test', 'non-existent-prog', 'DD National', '2026-10-13', '10:00', '11:00', 'Scheduled')
                """
            )

    def test_program_positive_duration_check(self):
        """CHECK constraint must disallow non-positive program duration."""
        with self.assertRaises(sqlite3.IntegrityError):
            self.conn.execute(
                """
                INSERT INTO programs (id, name, channel, category, duration)
                VALUES ('prog-bad', 'Invalid Program', 'DD National', 'News', 0)
                """
            )

    def test_schedule_status_check(self):
        """CHECK constraint must disallow invalid schedule status."""
        self.conn.execute(
            """
            INSERT INTO programs (id, name, channel, category, duration)
            VALUES ('prog-1', 'Valid Program', 'DD National', 'News', 30)
            """
        )
        with self.assertRaises(sqlite3.IntegrityError):
            self.conn.execute(
                """
                INSERT INTO schedules (id, program_id, channel, broadcast_date, start_time, end_time, status)
                VALUES ('sch-1', 'prog-1', 'DD National', '2026-10-13', '10:00', '10:30', 'InvalidStatusXYZ')
                """
            )


if __name__ == "__main__":
    unittest.main()
