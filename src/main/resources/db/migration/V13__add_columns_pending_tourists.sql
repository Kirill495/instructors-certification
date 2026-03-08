ALTER TABLE instructors_grades.pending_tourists
    ADD COLUMN IF NOT EXISTS gender VARCHAR(6),
    ADD COLUMN IF NOT EXISTS kind_of_tourism int references kinds_of_tourism(id),
    ADD COLUMN IF NOT EXISTS grade int references grades(id),
    ADD COLUMN IF NOT EXISTS tourist int references tourists(id),
    ADD COLUMN IF NOT EXISTS certification_id varchar(10);