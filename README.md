# Instructors Assignments — Инструкторские назначения

Веб-приложение для учёта инструкторских званий в спортивном туризме. Позволяет вести базу туристов, управлять справочниками (звания, виды туризма) и работать с протоколами присвоения званий.

## Стек технологий

- **Backend:** Java 21, Spring Boot 4, Spring MVC, Spring Data JPA
- **Шаблонизатор:** Thymeleaf
- **База данных:** PostgreSQL + Flyway (миграции)
- **Сборка:** Maven
- **Экспорт:** Apache POI (Excel)
- **Маппинг:** MapStruct + Lombok

## Предварительные требования

- Java 21+
- PostgreSQL 14+ (запущенный на `localhost:5432`)
- Maven (или использовать `./mvnw`)

## Запуск

### 1. Настройка базы данных

Создайте базу данных:

```sql
CREATE DATABASE instructors;
```

Схема и таблицы создаются автоматически через Flyway при первом запуске.

### 2. Настройка переменных окружения

Создайте файл `.env` в корне проекта (или задайте переменные окружения):

```env
DB_URL=jdbc:postgresql://localhost:5432/instructors
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

При необходимости создайте `config/secrets.yaml` на основе шаблона `config/secrets.yaml.template`.

### 3. Сборка и запуск

```bash
./mvnw spring-boot:run
```

Приложение будет доступно по адресу: [http://localhost:8080](http://localhost:8080)

Для сборки WAR-файла (деплой на сервер):

```bash
./mvnw clean package
```

## Структура проекта

```
src/main/java/org/tourism/instructors/
├── api/
│   ├── catalog/          # Контроллеры: звания, виды туризма
│   ├── protocol/         # Контроллеры: протоколы
│   ├── tourist/          # Контроллеры: туристы (MVC + REST)
│   ├── reports/          # Контроллер экспорта в Excel
│   └── home/             # Главная страница (дашборд)
├── application/
│   ├── catalog/          # Бизнес-логика: справочники
│   ├── protocol/         # Бизнес-логика: протоколы
│   ├── tourist/          # Бизнес-логика: туристы
│   └── reports/          # Бизнес-логика: отчёты
└── domain/
    ├── catalog/          # Сущности и репозитории: Grade, KindOfTourism
    ├── protocol/         # Сущности и репозитории: Protocol, ProtocolContent
    └── tourist/          # Сущности и репозитории: Tourist

src/main/resources/
├── templates/            # Thymeleaf HTML-шаблоны
├── static/js/            # JavaScript
└── db/migration/         # Flyway SQL-миграции
```

## Основные разделы приложения

| Раздел | URL | Описание |
|--------|-----|----------|
| Главная | `/` | Дашборд со статистикой |
| Протоколы | `/protocols` | Список, просмотр, редактирование протоколов |
| Туристы | `/tourists` | База туристов |
| Звания | `/catalog/grades` | Справочник инструкторских званий |
| Виды туризма | `/catalog/kinds-of-tourism` | Справочник видов туризма |

## API

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/tourists` | Список туристов (JSON, с пагинацией) |
| `GET` | `/api/tourists/search?query=` | Поиск туристов |
| `GET` | `/api/report/protocols` | Выгрузка протоколов в Excel (.xlsx) |

## База данных

Схема: `instructors_grades`

Основные таблицы:

- `tourists` — туристы (ФИО, дата рождения, контакты)
- `grades` — звания (название, срок действия в годах)
- `kinds_of_tourism` — виды туризма
- `protocols` — протоколы (номер, дата, номер приказа)
- `protocols_content` — содержимое протоколов (турист, вид туризма, звание, решение)

Справочники поддерживают мягкое удаление через поле `inactive`.

## Миграции (Flyway)

| Файл | Описание |
|------|----------|
| `V1__initial_schema.sql` | Начальная схема БД |
| `V2__insert_grades.sql` | Начальные данные: звания |
| `V3__insert_kinds_of_tourism.sql` | Начальные данные: виды туризма |
| `V4__add_inactive_field_to_kinds_of_tourism.sql` | Мягкое удаление для видов туризма |
| `V5__add_inactive_to_grades.sql` | Мягкое удаление для званий |
| `V6__alter_grades_set_default_expires_field.sql` | Срок действия звания |
| `V7__alter_ids_constraints.sql` | Исправление ограничений на ID |