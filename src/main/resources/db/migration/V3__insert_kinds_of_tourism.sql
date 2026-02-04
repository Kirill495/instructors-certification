INSERT INTO instructors_grades.kinds_of_tourism (title)
SELECT * FROM (VALUES
    ('вело'),
    ('водный'),
    ('горный'),
    ('конный'),
    ('лыхный'),
    ('пешеходный'),
    ('шк.тур'))
AS data(title)
WHERE NOT EXISTS (SELECT 1 FROM instructors_grades.kinds_of_tourism)