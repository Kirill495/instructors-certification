ALTER TABLE grades
    ADD inactive BOOLEAN DEFAULT FALSE;

ALTER TABLE grades
    ALTER COLUMN title TYPE VARCHAR(255) USING (title::VARCHAR(255));

UPDATE grades set inactive = true where title not in ('Инструктор СТ', 'Старший инструктор СТ', 'Организатор СТ')