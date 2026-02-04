INSERT INTO instructors_grades.grades(title, expires_in)
SELECT * FROM (VALUES
    ('Инструктор СТ', 5),
    ('Старший инструктор СТ', 10),
    ('Старший инструктор-методист', null),
    ('рекомендован для присвоения звания инструктора международного класса', null),
    ('Инструктор международного класса', null),
    ('Инс-проводник б/к', null),
    ('Организатор СТ', 5)
) AS data(title, expires_in)
WHERE NOT EXISTS (SELECT 1 FROM instructors_grades.grades)
