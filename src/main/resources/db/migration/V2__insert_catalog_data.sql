INSERT INTO instructors_grades.grades(id, title, expires_in)
SELECT *
FROM (VALUES (1, 'Организатор СТ', 5),
             (2, 'Инструктор СТ', 5),
             (3, 'Старший инструктор СТ', 10)) AS data(id, title, expires_in)
ON CONFLICT DO NOTHING;

INSERT INTO instructors_grades.kinds_of_tourism (id, title)
SELECT *
FROM (VALUES (1, 'горный'),
             (2, 'пешеходный'),
             (3, 'водный'),
             (4, 'лыжный'),
             (5, 'вело'),
             (6, 'конный'))
         AS data(title)
ON CONFLICT DO NOTHING;
