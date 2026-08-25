-- Временные данные для ручной проверки registry, пока нет ingest.
-- НЕ миграция: запускать вручную через
--   docker compose exec -T db psql -U <PUB_APP_USER> -d <PUB_DB_NAME> -f - < test-data.sql
-- Файл повторно запускаемый: начинается с очистки таблицы.

DELETE FROM publication.published_assignments;

-- Протокол 101, номер '15' — три присвоения.
-- row_num вставляется намеренно вперемешку (3, 1, 2), чтобы ORDER BY в запросе
-- что-то доказывал: при вставке по порядку сломанная сортировка неотличима от рабочей.

INSERT INTO publication.published_assignments
    (protocol_id, protocol_date, order_number, protocol_number, row_num,
     last_name, first_name, middle_name,
     grade, kind_of_tourism, club, assignment_date, valid_until)
VALUES
    (101, DATE '2026-03-14', '7', '15', 3,
     'Сидоров', 'Пётр', 'Петрович',
     'Инструктор-проводник', 'горный', 'Вертикаль', DATE '2026-03-14', DATE '2029-03-14');

-- Средний ряд: все три nullable-колонки пустые (middle_name, club, valid_until).
-- Единственная строка, на которой видно, что маппер не падает на NULL.
INSERT INTO publication.published_assignments
    (protocol_id, protocol_date, order_number, protocol_number, row_num,
     last_name, first_name, middle_name,
     grade, kind_of_tourism, club, assignment_date, valid_until)
VALUES
    (101, DATE '2026-03-14', '7', '15', 1,
     'Петрова', 'Анна', NULL,
     'Инструктор детско-юношеского туризма', 'пешеходный', NULL, DATE '2026-03-14', NULL);

INSERT INTO publication.published_assignments
    (protocol_id, protocol_date, order_number, protocol_number, row_num,
     last_name, first_name, middle_name,
     grade, kind_of_tourism, club, assignment_date, valid_until)
VALUES
    (101, DATE '2026-03-14', '7', '15', 2,
     'Иванов', 'Иван', 'Иванович',
     'Инструктор-проводник', 'водный', 'Аква', DATE '2026-03-14', DATE '2029-03-14');

-- Протокол 102, номер '16' — существует только чтобы WHERE было что отсеивать.
INSERT INTO publication.published_assignments
    (protocol_id, protocol_date, order_number, protocol_number, row_num,
     last_name, first_name, middle_name,
     grade, kind_of_tourism, club, assignment_date, valid_until)
VALUES
    (102, DATE '2026-04-02', '9', '16', 1,
     'Кузнецов', 'Алексей', 'Викторович',
     'Инструктор детско-юношеского туризма', 'лыжный', 'Метелица', DATE '2026-04-02', DATE '2029-04-02');
