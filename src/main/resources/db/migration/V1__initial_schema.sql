CREATE SCHEMA IF NOT EXISTS instructors_grades;

-- туристы
create table instructors_grades.tourists
(
    id               int generated always as identity primary key,
    first_name       VARCHAR(150),
    last_name        VARCHAR(150),
    middle_name      VARCHAR(150),
    gender           VARCHAR(6),
    date_of_birth    date,
    phone_number     varchar(100),
    email            varchar(100),
    certification_id varchar(10)
);

-- виды туризма
create table instructors_grades.kinds_of_tourism
(
    id    int generated always as identity primary key,
    title varchar
);

-- звания
create table instructors_grades.grades
(
    id    int generated always as identity primary key,
    title varchar
);

-- протоколы
create table instructors_grades.protocols
(
    id           int generated always as identity primary key,
    number       varchar(12),
    date         date,
    order_number varchar(12)
);

-- Табличная часть протокола "Туристы"
create table instructors_grades.protocols_content
(
    protocol_id      int references instructors_grades.protocols (id) ON DELETE CASCADE,
    row_num          int CHECK (row_num > 0),
    tourist_id       int references instructors_grades.tourists (id),
    kind_of_tourism  int references instructors_grades.kinds_of_tourism (id),
    grade            int references instructors_grades.grades (id),
    certification_id varchar(10),
    club             varchar,
    decision_type    varchar,
    PRIMARY KEY (protocol_id, row_num)
);
