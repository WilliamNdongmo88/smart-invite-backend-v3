ALTER TABLE checkin_parameters ADD COLUMN IF NOT EXISTS duplicate_scans INTEGER DEFAULT 0;
