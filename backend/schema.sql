-- Digital Broadcast Monitoring SQLite Database Schema
-- Foreign key support enabled
PRAGMA foreign_keys = ON;

-- 1. Programs Table
CREATE TABLE IF NOT EXISTS programs (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    channel TEXT NOT NULL,
    category TEXT NOT NULL,
    language TEXT NOT NULL DEFAULT 'English',
    description TEXT DEFAULT '',
    duration INTEGER NOT NULL CHECK (duration > 0),
    video_status TEXT DEFAULT 'Optimal',
    audio_status TEXT DEFAULT 'Optimal',
    compliance_score INTEGER DEFAULT 98,
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT DEFAULT (datetime('now'))
);

-- 2. Schedules Table
CREATE TABLE IF NOT EXISTS schedules (
    id TEXT PRIMARY KEY,
    program_id TEXT NOT NULL,
    channel TEXT NOT NULL,
    broadcast_date TEXT NOT NULL, -- YYYY-MM-DD
    start_time TEXT NOT NULL,     -- HH:MM
    end_time TEXT NOT NULL,       -- HH:MM
    status TEXT NOT NULL DEFAULT 'Scheduled' CHECK (status IN ('Scheduled', 'Live', 'Completed', 'Cancelled', 'Rescheduled', 'Flagged')),
    remarks TEXT DEFAULT '',
    is_verified INTEGER NOT NULL DEFAULT 1,
    has_overlap INTEGER NOT NULL DEFAULT 0,
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (program_id) REFERENCES programs(id) ON DELETE CASCADE
);

-- 3. Alerts Table
CREATE TABLE IF NOT EXISTS alerts (
    id TEXT PRIMARY KEY,
    timestamp TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    severity TEXT NOT NULL CHECK (severity IN ('ANOMALY', 'WARNING', 'SUCCESS')),
    channel TEXT NOT NULL,
    program_title TEXT,
    schedule_id TEXT,
    is_simulated INTEGER NOT NULL DEFAULT 0,
    created_at TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE SET NULL
);

-- 4. Status Log Table
CREATE TABLE IF NOT EXISTS status_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    schedule_id TEXT NOT NULL,
    old_status TEXT,
    new_status TEXT NOT NULL,
    changed_at TEXT DEFAULT (datetime('now')),
    remarks TEXT,
    FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_schedules_date_channel ON schedules(broadcast_date, channel);
CREATE INDEX IF NOT EXISTS idx_schedules_program ON schedules(program_id);
CREATE INDEX IF NOT EXISTS idx_schedules_status ON schedules(status);
CREATE INDEX IF NOT EXISTS idx_alerts_severity ON alerts(severity);
CREATE INDEX IF NOT EXISTS idx_status_log_schedule ON status_log(schedule_id);
