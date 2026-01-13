ALTER TABLE ticket_logs
RENAME COLUMN changed_by_user_id TO user_id;

ALTER TABLE ticket_logs
    ADD COLUMN client_id BIGINT,
    ADD COLUMN system_id BIGINT;

ALTER TABLE ticket_logs
ADD CONSTRAINT fk_ticket_logs_client
FOREIGN KEY (client_id) REFERENCES clients(id);

ALTER TABLE ticket_logs
ADD CONSTRAINT fk_ticket_logs_system
FOREIGN KEY (system_id) REFERENCES systems(id);

CREATE INDEX idx_ticket_logs_client_id ON ticket_logs (client_id);
CREATE INDEX idx_ticket_logs_system_id ON ticket_logs (system_id);