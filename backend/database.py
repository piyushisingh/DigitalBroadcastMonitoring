import os
import sqlite3
from flask import g, has_app_context

DEFAULT_DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "broadcast_monitoring.db")
SCHEMA_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "schema.sql")


def get_db(db_path=None):
    """
    Returns an active SQLite database connection with row factory
    and foreign key constraints enabled.
    """
    target_path = db_path or (g.get("db_path") if has_app_context() else None) or DEFAULT_DB_PATH

    if has_app_context():
        if "_database" not in g:
            g._database = sqlite3.connect(target_path)
            g._database.row_factory = sqlite3.Row
            g._database.execute("PRAGMA foreign_keys = ON;")
        return g._database

    conn = sqlite3.connect(target_path)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON;")
    return conn


def close_db(e=None):
    """Closes the current database connection if one was opened in this app context."""
    if has_app_context():
        db = g.pop("_database", None)
        if db is not None:
            db.close()


def init_db(db_path=None):
    """Executes schema.sql against the database to create all tables and indexes."""
    target_path = db_path or DEFAULT_DB_PATH
    os.makedirs(os.path.dirname(os.path.abspath(target_path)), exist_ok=True)

    conn = sqlite3.connect(target_path)
    try:
        conn.execute("PRAGMA foreign_keys = ON;")
        with open(SCHEMA_PATH, "r", encoding="utf-8") as f:
            conn.executescript(f.read())
        conn.commit()
    finally:
        conn.close()


def row_to_dict(row):
    """Convert a sqlite3.Row object to a standard Python dictionary."""
    if row is None:
        return None
    return dict(row)


def rows_to_dict_list(rows):
    """Convert a list of sqlite3.Row objects to standard Python dictionaries."""
    return [dict(r) for r in rows]
