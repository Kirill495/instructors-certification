-- Revision info table (one row per transaction that modifies an audited entity)
CREATE TABLE instructors_grades.revinfo
(
    rev              SERIAL PRIMARY KEY,
    username         VARCHAR(250),
    revtstmp         TIMESTAMP NOT NULL,
    operation_source VARCHAR(20)
);

-- Audit table for tourists (one row per revision of each tourist)
CREATE TABLE instructors_grades.tourists_aud
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

CREATE TABLE instructors_grades.contact_info_aud
(
    id         INT,
    rev        INT NOT NULL REFERENCES instructors_grades.revinfo (rev),
    revtype    SMALLINT, -- 0 = INSERT, 1 = UPDATE, 2 = DELETE
    type       VARCHAR(50),
    value      VARCHAR(100),
    details    VARCHAR,
    tourist_id INTEGER,  -- plain column, no FK: referenced tourist may be deleted
    PRIMARY KEY (id, rev)
);