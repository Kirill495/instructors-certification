CREATE TABLE IF NOT EXISTS instructors_grades.pending_tourists (
    id          SERIAL PRIMARY KEY,
    chat_id     BIGINT       NOT NULL,
    tg_username VARCHAR(255),
    last_name   VARCHAR(150) NOT NULL,
    first_name  VARCHAR(150) NOT NULL,
    middle_name VARCHAR(150),
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    date_of_birth DATE,
    email         VARCHAR(255),
    phone_number  VARCHAR(50),

    gender           VARCHAR(6),
    kind_of_tourism  INT REFERENCES kinds_of_tourism(id),
    grade            INT REFERENCES grades(id),
    tourist          INT REFERENCES tourists(id),
    certification_id VARCHAR(10),

    created_at  TIMESTAMP NOT NULL DEFAULT NOW()

);