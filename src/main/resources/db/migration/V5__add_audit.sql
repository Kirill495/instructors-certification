-- Revision info table (one row per transaction that modifies an audited entity)
CREATE TABLE IF NOT EXISTS instructors_grades.revinfo
(
    rev              SERIAL PRIMARY KEY,
    username         VARCHAR(250),
    revtstmp         TIMESTAMP NOT NULL,
    operation_source VARCHAR(20)
);

-- Audit table for tourists (one row per revision of each tourist)
CREATE TABLE IF NOT EXISTS instructors_grades.tourists_aud
(
    id               INT,
    rev              INT NOT NULL REFERENCES instructors_grades.revinfo (rev),
    revtype          SMALLINT, -- 0 = INSERT, 1 = UPDATE, 2 = DELETE
    first_name       VARCHAR(150),
    last_name        VARCHAR(150),
    middle_name      VARCHAR(150),
    gender           VARCHAR(6),
    date_of_birth    DATE,
    phone_number     VARCHAR(100),
    email            VARCHAR(100),
    certification_id VARCHAR(10),
    PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS instructors_grades.contact_info_aud
(
    id         INT,
    rev        INT NOT NULL REFERENCES instructors_grades.revinfo (rev),
    revtype    SMALLINT, -- 0 = INSERT, 1 = UPDATE, 2 = DELETE
    type       VARCHAR(50),
    value      VARCHAR(100),
    details    VARCHAR,
    tourist_id INTEGER,
    PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS instructors_grades.protocols_aud
(
    id           INT,
    rev          INT NOT NULL REFERENCES instructors_grades.revinfo(rev),
    revtype      SMALLINT, -- 0 = INSERT, 1 = UPDATE, 2 = DELETE
    number       VARCHAR(12),
    date         DATE,
    order_number VARCHAR(12),
    status       VARCHAR(20),
    PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS instructors_grades.protocols_content_aud
(
    id INT,
    rev INT NOT NULL REFERENCES instructors_grades.revinfo(rev),
    revtype SMALLINT, -- 0 = INSERT, 1 = UPDATE, 2 = DELETE
    tourist_id       INT,
    kind_of_tourism  INT,
    grade            INT,
    certification_id VARCHAR(10),
    club             VARCHAR,
    decision_type    VARCHAR,
    primary key (id, rev)
);
