CREATE TABLE instructors_grades.pending_tourists (
    id          SERIAL PRIMARY KEY,
    chat_id     BIGINT       NOT NULL,
    tg_username VARCHAR(255),
    last_name   VARCHAR(150) NOT NULL,
    first_name  VARCHAR(150) NOT NULL,
    middle_name VARCHAR(150),
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);