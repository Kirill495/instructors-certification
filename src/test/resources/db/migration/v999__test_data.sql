-- insert into instructors_grades.grades(id, title, expires_in, inactive) values
--                                                                            (1, 'Организатор СТ', 5, false),
--                                                                            (2, 'Инструктор СТ', 5, false),
--                                                                            (3, 'Старший инструктор СТ', 5, false);
-- insert into instructors_grades.kinds_of_tourism(id, title, inactive) values
--                                                                          (1, 'Пеший', false),
--                                                                          (2, 'Горный', false),
--                                                                          (3, 'Водный', false);

insert into instructors_grades.tourists(id, first_name, last_name, middle_name, gender, date_of_birth, certification_id) values
(1, 'Алексей', 'Толстой', 'Николаевич', 'MALE', '1883-01-10', '1000'),
(2, 'Владимир', 'Набоков', 'Владимирович', 'MALE', '1899-04-22', '1001'),
(3, 'Марина', 'Цветаева', 'Ивановна', 'FEMALE', '1892-10-08', '1002'),
(4, 'Анна', 'Ахматова', 'Андреевна', 'FEMALE', '1889-06-23', '1003');

insert into instructors_grades.protocols(id, number, date, order_number, status) values
(1, '100', '1990-01-01', '100', 'FINALIZED'),
(2, '101', '1990-02-01', '101', 'FINALIZED');

insert into instructors_grades.protocols_content(protocol_id, row_num, tourist_id, kind_of_tourism, grade, certification_id, decision_type) values
(1, 1, 1, 1, 1, '1000', 'Присвоение'),
(1, 2, 2, 1, 1, '1001', 'Присвоение')