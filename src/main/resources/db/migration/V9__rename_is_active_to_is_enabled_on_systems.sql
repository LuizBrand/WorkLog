ALTER TABLE systems
    RENAME COLUMN is_active TO is_enabled;

ALTER TABLE systems
    ALTER COLUMN is_enabled SET DEFAULT TRUE;