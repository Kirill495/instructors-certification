[![Quality gate Status](https://sonarcloud.io/api/project_badges/quality_gate?project=Kirill495_instructors-certification)](https://sonarcloud.io/summary/new_code?id=Kirill495_instructors-certification)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Kirill495_instructors-certification&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=Kirill495_instructors-certification)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=Kirill495_instructors-certification&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=Kirill495_instructors-certification)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Kirill495_instructors-certification&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=Kirill495_instructors-certification)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Kirill495_instructors-certification&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=Kirill495_instructors-certification)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Kirill495_instructors-certification&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Kirill495_instructors-certification)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Kirill495_instructors-certification&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=Kirill495_instructors-certification)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Kirill495_instructors-certification&metric=bugs)](https://sonarcloud.io/summary/new_code?id=Kirill495_instructors-certification)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Kirill495_instructors-certification&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=Kirill495_instructors-certification)


# Instructors Assignments — Аттестация инструкторов

Веб-приложение для учёта инструкторских званий в спортивном туризме. Позволяет вести базу инструкторов, управлять справочниками (звания, виды туризма) и работать с протоколами присвоения званий.

## Стек технологий

- **Backend:** Java 21, Spring Boot 4, Spring MVC, Spring Data JPA, Spring Security
- **Шаблонизатор:** Thymeleaf
- **База данных:** PostgreSQL + Flyway (миграции)
- **Кэш:** Spring Cache + Ehcache (JCache)
- **Аудит:** Hibernate Envers (история изменений сущностей)
- **Telegram-бот:** TelegramBots 6.9.7.1
- **Сборка:** Maven
- **Экспорт:** Apache POI (Excel)
- **Маппинг:** MapStruct + Lombok
- **Тестирование:** Testcontainers (PostgreSQL)

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
│   ├── pending/          # Контроллеры: ожидающие туристы (MVC + REST)
│   ├── user/             # Контроллеры: управление пользователями
│   ├── security/         # Контроллер входа, конфигурация Spring Security
│   ├── reports/          # Контроллер экспорта в Excel
│   ├── enums/            # REST-контроллер для enum-справочников
│   ├── bot/              # Webhook-контроллер Telegram-бота
│   └── home/             # Главная страница (дашборд)
├── application/
│   ├── catalog/          # Бизнес-логика: справочники
│   ├── protocol/         # Бизнес-логика: протоколы
│   ├── tourist/          # Бизнес-логика: туристы
│   ├── pending/          # Бизнес-логика: ожидающие туристы
│   ├── user/             # Бизнес-логика: пользователи
│   └── reports/          # Бизнес-логика: отчёты
├── domain/
│   ├── catalog/          # Сущности и репозитории: Grade, KindOfTourism
│   ├── protocol/         # Сущности и репозитории: Protocol, ProtocolContent
│   ├── tourist/          # Сущности и репозитории: Tourist
│   └── user/             # Сущности и репозитории: User, Role
├── infrastructure/
│   ├── audit/            # Конфигурация Hibernate Envers
│   └── logging/          # Аспекты логирования
└── config/               # Конфигурационные бины (кэш, и др.)

src/main/resources/
├── templates/            # Thymeleaf HTML-шаблоны
├── static/js/            # JavaScript
└── db/migration/         # Flyway SQL-миграции
```

## Основные разделы приложения

| Раздел | URL | Описание |
|--------|-----|----------|
| Вход | `/login` | Форма авторизации |
| Главная | `/` | Дашборд со статистикой |
| Протоколы | `/protocols` | Список, просмотр, редактирование протоколов |
| Туристы | `/tourists` | База туристов |
| Ожидающие | `/pending` | Туристы, ожидающие обработки (из Telegram-бота) |
| Профиль | `/profile` | Профиль текущего пользователя |
| Пользователи | `/admin/users` | Управление пользователями (только для ADMIN) |
| Звания | `/catalog/grades` | Справочник инструкторских званий |
| Виды туризма | `/catalog/kinds-of-tourism` | Справочник видов туризма |

## API

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/tourists` | Список туристов (JSON, с пагинацией) |
| `GET` | `/api/tourists/search?query=` | Поиск туристов |
| `GET` | `/api/report/protocols` | Выгрузка протоколов в Excel (.xlsx) |
| `GET` | `/api/pending` | Список ожидающих туристов (JSON) |
| `GET` | `/api/enums/{enumClass}` | Значения enum-справочника |
| `POST` | `/bot` | Webhook-эндпоинт Telegram-бота |

## База данных

Схема: `instructors_grades`

Основные таблицы:

- `tourists` — туристы (ФИО, дата рождения, контакты)
- `grades` — звания (название, срок действия в годах)
- `kinds_of_tourism` — виды туризма
- `protocols` — протоколы (номер, дата, номер приказа)
- `protocols_content` — содержимое протоколов (турист, вид туризма, звание, решение)
- `users` — пользователи приложения (логин, пароль, роль)
- `pending_tourists` — туристы, поступившие через Telegram-бот, ожидающие обработки
- `revinfo` — служебная таблица аудита Hibernate Envers (история изменений)

Справочники поддерживают мягкое удаление через поле `inactive`.

## Миграции (Flyway)

| Файл | Описание |
|------|----------|
| `V1__initial_schema.sql` | Начальная схема БД |
| `V2__insert_catalog_data.sql` | Начальные данные: звания и виды туризма |
| `V3__add_users_and_roles.sql` | Таблицы пользователей и ролей |
| `V4__add_pending_tourists.sql` | Таблица ожидающих туристов (Telegram-бот) |
| `V5__add_audit.sql` | Таблицы аудита Hibernate Envers |