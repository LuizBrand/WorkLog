ALTER TABLE tickets
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE work_log_history (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    changed_by_user_id BIGINT NOT NULL,
    change_group_id UUID NOT NULL,
    field_changed VARCHAR(100) NOT NULL,
    field_type VARCHAR(20) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    change_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_worklog_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE,
    CONSTRAINT fk_worklog_user FOREIGN KEY (changed_by_user_id) REFERENCES users (id)
);

CREATE INDEX idx_worklog_ticket_id ON work_log_history (ticket_id);
CREATE INDEX idx_worklog_group_id ON work_log_history (change_group_id);