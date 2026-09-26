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

Учёт инструкторских званий в спортивном туризме: база инструкторов, справочники (звания, виды
туризма), протоколы присвоения званий и публичный реестр выданных аттестаций.

Система состоит из **двух сервисов**. Монолит (`instructors-app`) — внутренняя рабочая среда с
авторизацией, где протоколы создаются и утверждаются. Сервис публикации (`publication-service`) —
отдельное приложение с собственной базой, которое отдаёт наружу только сведения об уже присвоенных
званиях. Связь между ними односторонняя, через Kafka.

## Архитектура

### Модули Maven

| Модуль | Упаковка | Назначение |
|--------|----------|------------|
| `instructors` (корень) | `pom` | агрегатор, `dependencyManagement` и `pluginManagement` |
| `instructors-app` | `war` | монолит: UI, админка, Telegram-бот, публикация протоколов в Kafka |
| `publication-service` | `jar` | потребитель снапшотов, публичный реестр |
| `publication-contract` | `jar` | контракт сообщения, общий для продюсера и консьюмера |

### Зачем разделение

Причина одна и она про персональные данные. `ProtocolContent` ссылается на `Tourist`, у которого есть
`contactinfo` — телефоны и Telegram-данные. У `Protocol.status` есть значение `DRAFT`, которое наружу
попадать не должно. Пока публикация живёт внутри монолита, «не показать телефон» — вопрос дисциплины:
достаточно одной неосторожной правки маппера. После разделения это физическая гарантия: у сервиса
публикации таких данных нет в базе, и утечь им неоткуда.

### Поток сообщений

```
instructors-app                      Kafka                     publication-service
───────────────                      ─────                     ───────────────────
протокол утверждён
   │
   ├─ INSERT в published_protocols_outbox   (та же транзакция)
   │
ProtocolOutboxRelay ──► protocols.snapshots ──► ProtocolSnapshotListener
   (раз в секунду,        key = protocolId          │
    с подтверждением)     cleanup.policy=compact    ├─ DELETE по ключу + INSERT
                                                    │  (одна транзакция)
ProtocolOutboxCleaner                               └─ ошибка разбора ──►
   (удаляет отправленные                               protocols.snapshots-dlt
    старше N суток)
```

Ключевые решения: передаётся **полный снапшот** протокола, а не дельта; ключ сообщения — id протокола,
что гарантирует порядок в пределах одного протокола; удаление и отзыв — через tombstone (сообщение с
`null` в значении); топик компактится, поэтому базу сервиса публикации можно уронить и пересобрать из
топика. Развёрнутое обоснование каждого пункта — в [docs/publication-service-design.md](docs/publication-service-design.md).

Дублирования записи в двух системах (Postgres + Kafka) не происходит: используется **transactional
outbox** — монолит пишет строку в outbox в той же транзакции, что и протокол, а отдельный relay
переносит её в Kafka с подтверждением.

## Стек технологий

- **Backend:** Java 21, Spring Boot 4, Spring MVC, Spring Data JPA, Spring Security
- **Обмен сообщениями:** Apache Kafka (Spring for Apache Kafka)
- **Шаблонизатор:** Thymeleaf (только монолит)
- **База данных:** PostgreSQL + Flyway, по одной базе на сервис
- **Кэш:** Spring Cache + Ehcache (JCache)
- **Аудит:** Hibernate Envers (история изменений сущностей)
- **Telegram-бот:** TelegramBots 6.9.7.1
- **Сборка:** Maven (multi-module), Spotless (google-java-format, AOSP), Checkstyle, JaCoCo
- **Экспорт:** Apache POI (Excel)
- **Маппинг:** MapStruct + Lombok
- **Тестирование:** JUnit 5, Mockito, Testcontainers 2.x (PostgreSQL, Kafka)
- **Инфраструктура:** Docker Compose, nginx как reverse proxy

## Предварительные требования

- Java 21+
- Docker и Docker Compose
- Maven не обязателен — в репозитории есть `./mvnw`

Отдельно поднимать PostgreSQL и Kafka не нужно: их поднимает compose.

## Запуск

### 1. Переменные окружения

Скопируйте `.env.template` в `.env` и заполните. Ключи:

```env
DB_HOST=localhost
DB_PORT=5432
POSTGRES_USER=
POSTGRES_PASSWORD=

# Основная БД
INSTR_DB_NAME=instructors
INSTR_APP_USER=
INSTR_APP_PASSWORD=

# БД публикации
PUB_DB_NAME=publication
PUB_APP_USER=
PUB_APP_PASSWORD=

# Какие compose-файлы использовать
COMPOSE_FILE=docker-compose.yml:docker-compose.dev.yml
COMPOSE_PATH_SEPARATOR=:
```

`COMPOSE_FILE` избавляет от необходимости передавать `-f` каждый раз. Для прод-режима оставьте в нём
только `docker-compose.yml`.

Затем скопируйте `config/secrets.yaml.template` в `config/secrets.yaml` и заполните — там учётные
данные приложения, токен Telegram-бота и логины/пароли начальных пользователей. Файл монтируется в
контейнер монолита, в образ не попадает.

### 2. Сборка и старт

```bash
./mvnw clean package
docker compose up --build
```

Остановка — `docker compose down`. С удалением данных (томы `postgres_data` и `kafka_data`) —
`docker compose down -v`.

### 3. Адреса

| Что | Через nginx | Напрямую (только dev) |
|-----|-------------|------------------------|
| Монолит | `http://localhost/` | `http://localhost:8081` |
| Сервис публикации | `http://localhost/pub/` | `http://localhost:8082` |
| PostgreSQL | — | `localhost:5432` |
| Kafka | — | `localhost:29092` |

Вход в монолит — `/login`, логин и пароль из `config/secrets.yaml` (`app.admin.*`, `app.user.*`).

### Что добавляет `docker-compose.dev.yml`

Базовый `docker-compose.yml` не публикует наружу ничего, кроме порта nginx. Dev-оверлей:

- открывает порты Postgres, Kafka и обоих приложений напрямую;
- включает отладку: **5004** — монолит, **5005** — сервис публикации (`suspend=n`, подключаться можно
  в любой момент через Remote JVM Debug);
- подкладывает монолиту тестовые данные — том с `instructors-app/src/test/resources/db/migration`
  монтируется как `/app/devdata`, и Flyway получает дополнительный `filesystem:` location.

### Запуск только монолита, без Docker

```bash
./mvnw -pl instructors-app spring-boot:run
```

Понадобится свой PostgreSQL и заполненный `config/secrets.yaml`. Учтите: без Kafka relay не сможет
отправлять сообщения, и записи будут накапливаться в outbox — для сквозной проверки публикации нужен
полный стек. Из корня проекта `spring-boot:run` не работает: корневой модуль имеет `packaging=pom`.

Профиль `test` для запуска приложения не годится — `application-test.yaml` лежит в
`src/test/resources` и существует только для `@ActiveProfiles("test")` в тестах.

## Сборка и проверки

```bash
./mvnw clean verify          # компиляция, unit- и интеграционные тесты, покрытие
./mvnw spotless:apply        # форматирование; spotless:check привязан к фазе validate
```

`verify` прогоняет интеграционные тесты через Testcontainers, поэтому **нужен запущенный Docker**.
Порог покрытия — 50% инструкций на модуль (JaCoCo, по объединённым данным unit- и IT-прогонов).

Правила многомодульной сборки и способы убедиться, что граница между модулями не размылась, —
в [docs/multi-module-conventions.md](docs/multi-module-conventions.md).

## Структура проекта

```
instructors/                        # корневой pom, агрегатор
├── publication-contract/           # контракт сообщения
│   └── org/tourism/publication/contract/
│       ├── ProtocolSnapshot.java   # снапшот протокола (версионированный)
│       ├── AssignmentSnapshot.java # одна строка протокола
│       └── TopicName.java          # имя топика
│
├── publication-service/
│   └── org/tourism/publication/
│       ├── ingest/                 # консьюмер: приём и применение снапшотов
│       │   ├── ProtocolSnapshotListener.java   # @KafkaListener, валидация ключа и версии
│       │   ├── ProtocolIngestService.java      # delete-by-key + insert в одной транзакции
│       │   └── IngestRetryListener.java
│       ├── registry/               # публичный read-side
│       └── infrastructure/kafka/   # конфигурация консьюмера, DLT, обработка ошибок
│
├── instructors-app/                # монолит
│   └── org/tourism/instructors/
│       ├── api/                    # контроллеры (все внутренние)
│       │   ├── catalog/            # звания, виды туризма
│       │   ├── protocol/           # протоколы (MVC + REST)
│       │   ├── tourist/            # туристы (MVC + REST)
│       │   ├── pending/            # заявки из Telegram-бота
│       │   ├── user/, security/    # пользователи, вход
│       │   ├── reports/            # выгрузка в Excel
│       │   ├── enums/, home/, bot/ # справочники enum, дашборд, webhook бота
│       │   └── util/
│       ├── application/
│       │   └── protocol/outbox/    # ProtocolOutboxWriter / Relay / Cleaner
│       ├── domain/                 # сущности и репозитории
│       └── infrastructure/
│           ├── kafka/              # продюсер, объявление топика
│           ├── audit/              # Hibernate Envers
│           └── logging/
│
├── db/                             # образ Postgres с инициализацией двух баз
├── nginx/                          # конфигурация reverse proxy
├── docker-compose.yml              # базовая конфигурация
├── docker-compose.dev.yml          # оверлей: порты, отладка, тестовые данные
└── docs/                           # проектная документация
```

## Разделы приложения (монолит)

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

### Монолит — внутренний, за авторизацией

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/tourists` | Список туристов (JSON, с пагинацией) |
| `GET` | `/api/tourists/search?query=` | Поиск туристов |
| `POST` | `/api/tourists` | Создание туриста |
| `GET` | `/api/protocols/search` | Поиск протоколов |
| `GET` | `/api/pending` | Список ожидающих туристов |
| `POST` | `/api/pending/{id}/link` | Привязать заявку к существующему туристу |
| `POST` | `/api/pending/{id}/unlink` | Отменить привязку |
| `GET` | `/api/report/protocols` | Выгрузка протоколов в Excel (.xlsx) |
| `GET` | `/api/enums/{enumClass}` | Значения enum-справочника |
| `POST` | `/bot` | Webhook-эндпоинт Telegram-бота |

### Сервис публикации — публичный реестр

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/v1/protocols/{number}` | Протокол по номеру со списком присвоенных званий |

Аутентификация публичного API пока не реализована — см. `docs/STATUS.md`. Согласованное решение:
API-ключи в заголовке, ключи хранятся в базе сервиса публикации.

## Базы данных

Две **отдельные базы** в одном экземпляре Postgres, не две схемы: у Postgres нет кросс-базовых
запросов без FDW, поэтому разные базы — настоящая граница изоляции, а разные схемы — лишь соглашение
об именовании. У каждого сервиса свой Flyway со своей `flyway_schema_history`.

### `instructors`, схема `instructors_grades`

| Таблица | Описание |
|---------|----------|
| `tourists` | туристы (ФИО, дата рождения) |
| `contact_info` | контакты туристов — телефоны, Telegram. Наружу не уходят никогда |
| `grades` | звания (название, срок действия в годах) |
| `kinds_of_tourism` | виды туризма |
| `protocols` | протоколы (номер, дата, номер приказа, статус) |
| `protocols_content` | содержимое протоколов (турист, вид туризма, звание, решение) |
| `users` | пользователи приложения; роль — колонка `role`, отдельной таблицы нет |
| `pending_tourists` | заявки из Telegram-бота, ожидающие обработки |
| `published_protocols_outbox` | transactional outbox: полезная нагрузка, `sent_at`, `attempts`, `dead_at` |
| `revinfo`, `*_aud` | история изменений (Hibernate Envers) |

Справочники поддерживают мягкое удаление через поле `inactive`.

### `publication`, схема `publication`

| Таблица | Описание |
|---------|----------|
| `published_assignments` | плоский реестр: протокол + строка присвоения, PK `(protocol_id, row_num)` |

Персональных данных, кроме ФИО, здесь нет по построению — контактов в снапшоте не передаётся.

## Миграции (Flyway)

### `instructors-app`

| Файл | Описание |
|------|----------|
| `V1__initial_schema.sql` | Начальная схема БД |
| `V2__insert_catalog_data.sql` | Начальные данные: звания и виды туризма |
| `V3__add_users_and_roles.sql` | Таблицы пользователей и ролей |
| `V4__add_pending_tourists.sql` | Таблица ожидающих туристов (Telegram-бот) |
| `V5__add_audit.sql` | Таблицы аудита Hibernate Envers |
| `V6__add_outbox.sql` | Таблица outbox и частичный индекс по неотправленным |
| `V7__add_dead_at_to_outbox.sql` | `dead_at` и `error_message` для неотправляемых записей |

В тестовом профиле и в dev-режиме дополнительно применяется repeatable-миграция
`R__dev_data.sql` из `src/test/resources/db/migration`.

### `publication-service`

| Файл | Описание |
|------|----------|
| `V1__initial_schema.sql` | Таблица `published_assignments` |

## Документация

- [docs/STATUS.md](docs/STATUS.md) — что сделано, что дальше и в каком порядке. Начинать отсюда.
- [docs/publication-service-design.md](docs/publication-service-design.md) — архитектура сервиса
  публикации: передача состояния через Kafka, tombstone'ы, компактизация, outbox, границы полезной
  нагрузки.
- [docs/multi-module-conventions.md](docs/multi-module-conventions.md) — правила многомодульной
  сборки и как проверить, что граница между модулями не размылась.
