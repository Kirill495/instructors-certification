CREATE TABLE instructors_grades.contact_info (
    id SERIAL PRIMARY KEY,
    type VARCHAR(50),
    value VARCHAR(100),
    details VARCHAR,
    tourist_id INTEGER REFERENCES instructors_grades.tourists(id)
);

INSERT INTO instructors_grades.contact_info(tourist_id, type, value)
(SELECT
    tourists.id as tourist_id,
    'EMAIL' as type,
    tourists.email as value
FROM
    instructors_grades.tourists as tourists
WHERE
    tourists.email is not null
    and tourists.email != ''
UNION ALL
 SELECT
     tourists.id as tourist_id,
     'PHONE_NUMBER' as type,
     tourists.phone_number as value
 FROM
     instructors_grades.tourists as tourists
 WHERE
     tourists.phone_number is not null
   and tourists.phone_number != '');
