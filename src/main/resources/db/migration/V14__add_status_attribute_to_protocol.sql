ALTER TABLE instructors_grades.protocols
    ADD IF NOT EXISTS status VARCHAR(20);

UPDATE instructors_grades.protocols SET status = 'FINALIZED' WHERE status is null;