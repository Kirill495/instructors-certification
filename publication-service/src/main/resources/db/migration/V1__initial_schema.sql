CREATE TABLE published_assignments (
    protocol_id     INT NOT NULL,
    protocol_date   DATE NOT NULL,
    order_number    VARCHAR NOT NULL,
    protocol_number VARCHAR NOT NULL,
    row_num         INT NOT NULL,
    last_name       VARCHAR NOT NULL,
    first_name      VARCHAR NOT NULL,
    middle_name     VARCHAR,
    grade           VARCHAR NOT NULL,
    kind_of_tourism VARCHAR NOT NULL,
    club            VARCHAR,
    assignment_date DATE NOT NULL,
    valid_until     DATE,
    PRIMARY KEY (protocol_id, row_num)
);