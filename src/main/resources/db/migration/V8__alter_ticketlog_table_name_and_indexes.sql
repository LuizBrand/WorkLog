ALTER TABLE work_log_history RENAME TO ticket_logs;

ALTER INDEX idx_worklog_ticket_id RENAME TO idx_ticket_logs_ticket_id;
ALTER INDEX idx_worklog_group_id RENAME TO idx_ticket_logs_group_id;