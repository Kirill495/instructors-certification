ALTER TABLE instructors_grades.pending_tourists
    ADD COLUMN date_of_birth DATE,
    ADD COLUMN email         VARCHAR(255),
    ADD COLUMN phone_number  VARCHAR(50);