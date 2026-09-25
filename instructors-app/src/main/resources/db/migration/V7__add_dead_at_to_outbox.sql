ALTER TABLE instructors_grades.published_protocols_outbox
    ADD COLUMN dead_at timestamptz,
    ADD COLUMN error_message VARCHAR;