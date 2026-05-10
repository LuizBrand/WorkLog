ALTER TABLE tickets ADD COLUMN priority VARCHAR(20);
UPDATE tickets SET priority = 'MEDIUM' WHERE priority IS NULL;
ALTER TABLE tickets ALTER COLUMN priority SET NOT NULL;
