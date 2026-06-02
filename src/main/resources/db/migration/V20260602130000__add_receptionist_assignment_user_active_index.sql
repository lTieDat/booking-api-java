CREATE INDEX idx_receptionist_assignments_user_active
    ON receptionist_assignments (user_id, is_active);
