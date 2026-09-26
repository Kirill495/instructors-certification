# Project Status

Current state of the monolith-to-services split and the ordered plan for the next work session.

Related: [Publication service design](publication-service-design.md),
[Multi-module conventions](multi-module-conventions.md).

**Last updated**: 2026-09-26

## Done and committed

### Spotless rollout

- google-java-format `1.36.0`, style `AOSP`, version pinned, `check` bound to the `validate` phase.
- All 122 java files reformatted in a formatting-only commit, recorded in `.git-blame-ignore-revs`.
  `blame.ignoreRevsFile` is **local git config** — re-run `git config blame.ignoreRevsFile
  .git-blame-ignore-revs` on any other machine. GitHub and GitLab pick the file up automatically.
- The IntelliJ google-java-format plugin defaults to the GOOGLE style (2 spaces) and had to be
  switched to AOSP (4 spaces) to match the build. Its version must track the version in the pom.
- `checkstyle.xml`: `RightCurlyAlone` relaxed from `alone` to `alone_or_singleline`.
  google-java-format collapses empty constructor bodies to `private X() {}` (Google Java Style
  §4.1.3), while google_checks forbids it. The two Google tools disagree; the formatter wins,
  because code layout can only have one owner.

### Multi-module split

- Root `instructors` (`packaging=pom`) aggregates `instructors-app` (the former monolith, war),
  `publication-service` and `publication-contract`.
- Monolith moved with `git mv`; 182 renames preserved, so `blame` and `log --follow` still work.
- Root pom reduced to `dependencyManagement` + `pluginManagement`.
- Flyway migrations restored to `src/main/resources/db/migration`. The `db/` level was dropped
  during the move, Flyway silently found no migrations, and with `ddl-auto: none` a fresh database
  would have started clean and failed on the first query. `fail-on-missing-locations: true` is now
  set so this class of breakage fails loudly at startup.
- Full build green: 340 tests passing.

### `publication-contract`

`ProtocolSnapshot` and `AssignmentSnapshot` written; module builds, spotless and checkstyle clean.
The payload and the reasoning behind every included and excluded field are in
[the design doc](publication-service-design.md#payload).

Jacoco does not apply to this module — it is declared only in the root `pluginManagement` and the
module does not opt in. It will apply to `publication-service` once that module declares it, which
is intended.

### Docker build repaired after the split (2026-08-18)

The monolith's Dockerfile had been silently broken by the module move — it still did
`COPY ../src ./src`, and the deploy builds on the VPS from source (`git pull` +
`docker compose build`), so the next deploy would have failed.

- Moved `app/Dockerfile` to `instructors-app/Dockerfile`. The build **context stays the repository
  root** (`context: .` in compose) because a multi-module build needs the parent pom, the contract
  module and the checkstyle config; only the `dockerfile:` path changes. Context and Dockerfile
  location are independent — that is what makes one Dockerfile per module workable.
- Build now copies every module's `pom.xml` (Maven cannot build the reactor model without them),
  plus `instructors-app/src` and `publication-contract/src`, and runs
  `mvn -pl instructors-app -am package`.
- `dependency:go-offline` plus the pom-copy layer trick replaced by a BuildKit cache mount
  (`RUN --mount=type=cache,target=/root/.m2/repository`), which keeps `~/.m2` outside the image
  layers so downloads survive pom edits and are shared between the two services.
- `-Dspotless.check.skip=true` added next to the existing checkstyle skip: formatting is a
  development-time gate and has no business running inside a release image.
- **`<packaging>war</packaging>` was lost in the split** (it had lived on the old root pom, which is
  now `packaging=pom`), so the module was quietly producing a jar. Restored.
- `instructors-app` now declares its own plugins (spring-boot, compiler, dependency, surefire,
  jacoco, failsafe); `pluginManagement` only configures, it does not activate.
- Verified: image builds, `instructors-app-0.0.1-SNAPSHOT.war` is present and executable, and the
  app boots as far as needing a database. Standalone `docker run` cannot go further — the JDBC url
  lives in `config/secrets.yaml`, which compose mounts.

### `publication-service` — skeleton done, starts and migrates

pom, `PublicationServiceApplication`, `application.yaml`, `V1__initial_schema.sql`, Dockerfile, and
the service in `docker-compose.yml` on port 8082. It starts, connects to its own database and Flyway
creates `publication.published_assignments`. No `ingest` or `registry` code yet.

Schema `publication`, table `published_assignments`, primary key `(protocol_id, row_num)` — the key
both enforces idempotent re-insert and indexes the `DELETE ... WHERE protocol_id = ?` that every
message performs.

Persistence is `spring-boot-starter-jdbc` with `JdbcClient`, **not** Spring Data JDBC: the latter is
built around aggregates with a single `@Id` and handles composite keys badly. The write path is a
delete plus a batch insert and the read path is a couple of selects — plain SQL fits better than
any mapping layer here.

Both defects recorded here on 2026-08-18 are fixed: the `classpath:db/migragion` typo in
`application.yaml`, and the CR-only line endings in `instructors-app/pom.xml` (now CRLF, normalised
to LF on commit). Note that the commit which introduced the CR-only version is already in history,
so *that* commit's diff of the pom stays unreadable; everything after it is clean.

### Compose wired to the new layout, verified on a clean volume (2026-08-21)

- `docker-compose.yml`: `app` now builds `dockerfile: instructors-app/Dockerfile` with `context: .`
  unchanged. The `app/` directory is gone.
- **Flyway verified from scratch**: `docker compose down -v` followed by `up -d --build db app`
  created the schema history table and applied all 5 migrations in order. This is the check the
  module move never got — on an already-migrated volume Flyway is a no-op, so the broken
  `db/migration` path would have stayed invisible.
- The datasource url moved from `config/secrets.yaml` into compose as `SPRING_DATASOURCE_URL`.
  `localhost` is correct when the app runs on the host and wrong inside a container, where it means
  the container's own loopback; between containers the address is the **service name** (`db:5432`),
  resolved by the compose network's DNS. Publishing `ports` on `db` does not help container-to-
  container traffic — it only exposes the port to the host. Keeping topology in compose and only
  secrets in `secrets.yaml` lets both run modes work from one file, because environment variables
  outrank imported config files in Spring's property order.
- Two traps met on the way, both worth remembering. A stray edit turned the bind mount source into
  `./db/ upinit.sh`; Docker **silently creates a missing bind-mount source as a directory**, then
  failed with "not a directory" only because the target inside the container is a file. Had the
  target been a directory, the service would have started with an empty mount and no error at all.
  The long mount syntax with `bind: { create_host_path: false }` turns this into an error — worth
  applying to the three single-file mounts (`init.sh`, `secrets.yaml`, `nginx.conf`).
  Second: `docker compose up` rebuilds an image only if it does not exist; it does not notice that
  the Dockerfile or the sources changed. After editing a Dockerfile, `--build` is required.
  `down -v` removes containers, networks and volumes, but never images.

### Known redundancy: the schema is declared three times

`hibernate.default_schema`, `@Table(schema = ...)` on all 8 entities, and `?currentschema=` in the
jdbc url. Only the first two do anything: pgjdbc's parameter is `currentSchema` (capital S) and the
driver silently ignores unknown parameters, so the url fragment is dead weight that misleads anyone
reading it. Drop it from `secrets.yaml` and from `SPRING_DATASOURCE_URL`. Collapsing the per-entity
`schema =` into the single `default_schema` is a separate, larger cleanup — 9 files to touch if the
schema is ever renamed.

### Second database, one Postgres instance (2026-08-22)

Both services now have their own database in the **same** Postgres container. The boundary the design
calls for is a separate *database* — Postgres cannot join across databases without FDW — and that
property holds inside one instance. A separate container would add resource and failure isolation,
which was never the requirement.

`db/` reorganised. Executable init scripts must sit flat in `/docker-entrypoint-initdb.d/` (the
entrypoint does not recurse into subdirectories) and run in lexicographic order, so only the SQL is
split per database:

```
db/initdb/01_instructors.sh   02_publication.sh     -> /docker-entrypoint-initdb.d/
db/sql/instructors/...        db/sql/publication/...
```

Each script creates its role, its database, then connects **to that database** to create the schema
— a schema cannot be created in a database other than the one psql is connected to, so two psql
invocations are unavoidable. Init scripts run only when the data directory is empty, i.e. only on a
fresh volume; `down -v` is required to re-run them, and idempotency guards in them are therefore
decorative.

`REVOKE ALL ON DATABASE ... FROM PUBLIC` added on both databases. `PUBLIC` is not a group but "every
role", and it is granted `CONNECT` and `TEMP` on every new database by default — so the pre-existing
`GRANT CONNECT ... TO app_user` was a no-op, and the internet-facing publication service could open a
session against the monolith's database. It could not read the data (the schema is owned by the other
role and `PUBLIC` has no `USAGE`), but it could enumerate the catalogs and consume connection slots,
and any future over-broad `GRANT` would have turned that into a leak.

Credentials now come from `.env` through compose `environment` (`SPRING_DATASOURCE_*`), which is the
same file the init scripts create those roles from — the password used to exist in two places and had
to be kept in sync by hand. `publication-service` has no `secrets.yaml` at all. The monolith keeps
one, deliberately: it also holds application-level secrets, and its datasource credentials stay there
as the default for running on the host, where compose's environment does not exist.

`.gitattributes` gained `*.sh text eol=lf`. Without it `core.autocrlf` would rewrite the new init
scripts to CRLF on the next checkout and the db image would fail with `/bin/bash^M: bad interpreter`
— breakage caused by a checkout, not by an edit.

### Kafka in compose, topic created (2026-08-23)

Single `apache/kafka:4.0.0` container in KRaft combined mode (`broker,controller`) — no ZooKeeper,
it was removed in Kafka 4.0. Three listeners, and the reason there must be three:

```
INTERNAL://kafka:9092       for app and publication, inside the compose network
HOST://localhost:29092      for a client on the Windows host (IDEA, console tools)
CONTROLLER://…:9093         broker↔controller, never advertised
```

Kafka answers a metadata request over the listener the client entered through, and returns *that*
listener's advertised address. One listener therefore cannot serve both audiences: the address would
be right for containers or for the host, never both. `KAFKA_LISTENERS` binds on `0.0.0.0` (the
container's eth0 address is assigned dynamically, so it cannot be written down);
`KAFKA_ADVERTISED_LISTENERS` carries the names clients must dial.

Verified with `kafka-metadata-quorum.sh describe --status`: leader 1, voters
`[CONTROLLER://kafka:9093]`, metadata log committed — the KRaft log that replaced ZooKeeper is live.
An empty `--list` with no error is a *good* sign: answering it requires a full metadata round trip.

Auto-creation is off, so the topic is declared as a `NewTopic` bean in the **monolith**
(`infrastructure/kafka`) — a topic belongs to whoever writes to it, and `cleanup.policy=compact` is a
design decision that belongs beside the code depending on it. An auto-created topic would silently
get `cleanup.policy=delete`, which is exactly wrong. Confirmed on the broker:
`Configs: cleanup.policy=compact`, 1 partition.

Partition count is deliberate, not a default: key→partition mapping changes if partitions are added
later, and for a compacted topic that leaves stale values stranded in the old partition.

### `registry` — read side done (2026-08-25)

`GET /api/v1/protocols/{number}` → `ProtocolResponse` with a nested list of assignments.
`ProtocolRegistry` uses `JdbcClient` and groups the flat rows in Java.

The public url is keyed by **number**, not by `protocol_id`: the id is an internal surrogate and must
not leak into a public contract. Numbers were checked in the real data and are unique, so the method
returns `Optional`. The response DTOs are separate types from `publication-contract` on purpose —
the Kafka payload is an internal contract between our own services and can be renegotiated; the HTTP
response is public and cannot.

Tested with `@JdbcTest` + Testcontainers + `@ServiceConnection`, fixture loaded via
`@Sql("/test-data.sql")`. The fixture is built to be able to fail: `row_num` is inserted out of order
(3, 1, 2) so a broken `ORDER BY` is visible, one row has all three nullable columns empty, and a
second protocol exists so `WHERE` has something to filter. A mocked `JdbcClient` would have proven
nothing here — the SQL string is the risky part, and it is neither compiled nor type-checked.

Schema resolution ended up as `spring.datasource.hikari.schema: publication` in `application.yaml`.
The schema is a property of the *application*, identical on a laptop, in a container and in a test —
so it belongs in the app's own config, not in an environment-specific jdbc url. The
`?currentSchema=` route worked in compose but left the test with no schema at all.

### Compose split into base and dev (2026-08-24)

`docker-compose.yml` now publishes only nginx's port 80. Everything that exists solely for local work
— `5432`, `29092`, `8081`, `8082`, `5005` and the JDWP `JAVA_TOOL_OPTIONS` — moved to
`docker-compose.dev.yml`, opted in via `COMPOSE_FILE` in `.env`. Commenting lines out before a deploy
was rejected as the mechanism: it is a manual step someone eventually forgets, and the failure is
silent. This way a forgotten flag breaks *local* debugging, which is noticed immediately.

Published ports bind `0.0.0.0`, so on the VPS they were reachable from the internet; containers never
needed them, since they reach each other over the compose network.

nginx gained `location /api/` → `publication:8080`. Note the absent trailing slash on `proxy_pass`:
with it, nginx would strip the matched `/api/` prefix and the controller would 404. nginx also
resolves upstream names at startup, so `publication` had to be added to its `depends_on` or nginx
refuses to start.

### `ingest` — write side done, verified end to end (2026-09-02)

A message now travels Kafka → deserialisation → validation → registry, and anything malformed lands
in a dead-letter topic with full diagnostics. Verified on the running stack, not only by reasoning:
a valid snapshot produced `Записан протокол 101. Строк: 1` and a row in `published_assignments`; a
snapshot whose key (`102`) disagreed with its payload (`protocolId: 101`) went straight to the DLT
with `kafka_dlt-exception-cause-fqcn: IncorrectProtocolIdException` and no retries.

**Write is delete-then-insert by `protocol_id` in one transaction**, never an upsert by
`(protocol_id, row_num)`. Rows do not only change, they disappear — an assignment removed from a
protocol would leave orphans from the previous, longer version. Full replacement is the only thing
that makes the row set exactly equal the snapshot, and it is also what makes redelivery harmless, so
no deduplication or processed-offset table is needed.

**The listener reads `protocolId` from the message key and compares it with the payload.** The key is
not redundant: a tombstone has a `null` payload, so the key is the only source of the identifier
there. A mismatch means a producer bug, and it fails loudly rather than corrupting the registry
quietly.

**The payload `version` is checked and an unknown value throws.** Under compaction messages never
expire, so a consumer deployed a year from now will still read what is written today. JSON is
lenient — unknown fields ignored, missing fields nulled — so without the check a changed field
meaning would be misread silently. `version` is therefore part of the meta-contract: its name and
type may never change, and the deserialiser must stay lenient about unknown properties, or a v2
message would fail to parse before anyone could read the version out of it.

#### Error policy is inverted from the usual shape

Default is **retry forever** (`FixedBackOff(5s, UNLIMITED_ATTEMPTS)`); only explicitly listed
producer bugs (`IncorrectMessageKeyException`, `IncorrectProtocolIdException`) go to the DLT, with no
retries. The obvious arrangement — a finite number of attempts, then the DLT — would shovel perfectly
valid snapshots into the DLT during a ten-minute Postgres outage. The rule that fell out of it:

> The DLT holds what is **wrong**, never what merely arrived at a **bad moment**.

That inversion also removed the need for a per-exception `setBackOffFunction`:
`UnsupportedSnapshotVersionException` falls into the default and blocks the partition until a
consumer that understands the new version is deployed, at which point the retry succeeds on its own.
It is one of the few errors where an unbounded retry is a feature rather than a hang.

Deserialisation failures need no entry in the list — `DeserializationException` and `ClassCastException`
are already in spring-kafka's default fatal set. Note that `ClassCastException` being fatal means a
*configuration* mistake (wrong deserialiser) sends valid messages to the DLT; the list is worth
re-reading occasionally.

Classification matched despite the exception arriving wrapped: spring-kafka unwraps
`ListenerExecutionFailedException` and walks the cause chain (`traverseCauses` is on by default).
The traversal only descends while the result is still the default, so the **outermost explicit**
classification wins.

Matching is by exact class, deliberately — a common supertype for "producer bug" was considered and
rejected in favour of an explicit list. The cost accepted: a new exception added later defaults to
retry-forever, i.e. a silently stuck partition, so classification must be decided when the exception
is written.

#### Dead-letter topic

Named **`protocols.snapshots-dlt`**. The default suffix in spring-kafka 4.0.3 is `-dlt`, not `.DLT`
— confirmed in `DeadLetterPublishingRecoverer`:
`(cr, e) -> new TopicPartition(cr.topic() + "-dlt", cr.partition())`. Getting this wrong cost an
evening, so it is written down.

Declared as a `NewTopic` bean in **publication-service**, unlike the main topic which belongs to the
producer: a DLT is not part of any contract, and the producer must not know the consumer rejects
things. Plain `cleanup.policy=delete` with an explicit `retention.ms` — compaction would let a second
failure for the same `protocolId` erase the record of the first, which is exactly the history the DLT
exists to keep, and retention is also the only mechanism that ever removes the personal data sitting
in those payloads.

Values are serialised **by type** (`DelegatingByTypeSerializer`): a parsed `ProtocolSnapshot` as JSON,
unparseable input as raw `byte[]`. Both branches have now fired in practice. The `byte[]` branch is
the important one — running the original bytes through a JSON serialiser would base64 them and
destroy the only evidence about a poison message.

`failIfSendResultIsError` (true by default) proved itself during the wrong-topic-name incident: the
DLT publish failed, the recoverer threw, the offset did not advance, and the record was retried
instead of lost. "Reached the DLT" and "offset advanced" are one event.

#### Jackson 2 and Jackson 3 coexist — use the Jackson 3 serialisers

Boot 4 uses Jackson 3 (`tools.jackson`), where `java.time` support is built in. spring-kafka 4.0.3
still ships the legacy `JsonSerializer` / `JsonDeserializer` on Jackson 2 (`com.fasterxml`), and
`jackson-datatype-jsr310` is not on the classpath because Boot has no use for it. The result is a
`InvalidDefinitionException: Java 8 date/time type java.time.LocalDate not supported by default` on
the first message with a date.

Use `JacksonJsonSerializer` / `JacksonJsonDeserializer` instead — same package, Jackson 3 underneath,
and **the same `spring.json.*` property names**, so the switch is one line in the yaml and one in the
producer config. Adding the Jackson 2 module was rejected: two Jackson generations with independent
date settings in one application will diverge eventually.

#### Known consequence: a stuck partition is silent

Retries are logged at DEBUG (`SeekUtils`), so a message caught in the unbounded retry loop produces
no ERROR and no WARN at default levels. The only signal is a growing consumer lag:
`kafka-consumer-groups.sh --describe --group publication-service` — `LAG` climbing while
`CURRENT-OFFSET` stands still. In production this is the metric to alert on.

### `ingest` under test on three levels (2026-09-04)

16 tests in the module, all green. The split is deliberate and the timings show why:

```
ProtocolSnapshotListenerTest              8   0.3 s   plain JUnit + Mockito, no Spring
ProtocolIngestServiceTest                 4   4.9 s   @JdbcTest + Testcontainers, no Kafka
ProtocolSnapshotListenerIntegrationTest   3   6.9 s   @SpringBootTest + Postgres + Kafka
ProtocolRegistryTest                      1   2.0 s
```

Each level tests what only it can, and mocking policy follows from that. In `ProtocolIngestService`
a mocked `JdbcClient` would prove nothing — the risk lives in an SQL string that neither the compiler
nor the type system checks — so that one needs a real database. In the listener the risk is branching
and validation, and `ProtocolIngestService` is a boundary worth not crossing, so a mock is right
there. Same project, opposite decisions, one criterion: where does the risk actually sit.

The listener tests assert `verifyNoInteractions(service)` on every throwing path. That is the half
people skip: `assertThrows` proves an exception was raised, not that it was raised *before* the
service was called. Without it the checks could migrate below `service.apply(...)` and every test
would stay green while bad data reached the registry.

An unknown `version` and a mismatched key are covered together in one case, pinning the order of the
checks: until the version is confirmed, `protocolId` from the payload is arbitrary bytes.

#### The integration test earned its keep immediately

It found a defect that manual testing structurally could not: `DltProducerConfig` built its
`ProducerFactory` from `kafkaProperties.buildProducerProperties()` alone, so the DLT producer dialled
the default `localhost:9092` instead of the broker.

`@ServiceConnection` does not set the `spring.kafka.bootstrap-servers` *property* — it registers a
`KafkaConnectionDetails` **bean**. Boot's own factories consult that bean; a hand-built factory reads
only properties. In compose the two agree, because `SPRING_KAFKA_BOOTSTRAP_SERVERS` is a real
environment variable — so the defect existed all along and was invisible, since the only environment
exercising it happened to use the one mechanism the code understood.

Fixed by injecting `KafkaConnectionDetails` and overriding `BOOTSTRAP_SERVERS_CONFIG` on top of the
properties map. The bean always exists: without `@ServiceConnection`, Boot auto-configures
`PropertiesKafkaConnectionDetails` over the same properties. **Rule: `KafkaProperties` is "what the
config says", `KafkaConnectionDetails` is "where to actually connect". Building a client by hand, ask
the latter.** The same applies to Boot's Docker Compose support and cloud service bindings, which
deliver the address the same way.

Worth noting how the failure presented: the DLT publish blocked for `max.block.ms`, threw,
the offset did not advance, and the handler retried forever — at DEBUG. Both tests simply timed out
with nothing anywhere. That is the "stuck partition is silent" consequence above, live. It also
failed *loudly* only by luck: had anything been listening on `localhost:9092` — the compose stack, a
stale container — the producer would have connected to the **wrong broker** with no error at all. A
default pointing at a plausible address is more dangerous than one pointing at nothing.

#### Test design decisions worth keeping

- **Negative assertions need a happens-after marker.** "The rows are unchanged" is true before the
  listener even wakes up. The DLT record is used as that marker: awaiting it proves processing
  finished, and asserting it proves the message was preserved rather than lost. One mechanism, both
  jobs.
- **The poison-message and tombstone tests are a pair.** After `ErrorHandlingDeserializer`, a
  malformed payload arrives as `value == null` — indistinguishable in shape from a tombstone. One
  test says "null deletes", the other says "unparseable bytes do not". Separately each admits an
  implementation that confuses them; together they prove the two are told apart. The seeded row
  matters: without it the poison test would assert that an empty table stayed empty.
- **The payload is a JSON literal, not a serialised object.** Serialising with the same Jackson that
  deserialises would test a round trip of our own configuration. The literal pins the wire format,
  which is what actually broke. Dates are asserted by value, too — parsing that *succeeded* is not
  parsing that is *correct*.
- Failed deserialisation goes to the DLT with no retries because `DeserializationException` is in
  spring-kafka's default fatal list. The test asserts the `kafka_dlt-exception-fqcn` header equals
  it — otherwise the test stays green when a message reaches the DLT for an unrelated reason. It also
  asserts the DLT value equals the original bytes, which is the only check of the `byte[]` branch of
  `DelegatingByTypeSerializer`.

#### Testcontainers migrated to 2.x

`postgresql` → `testcontainers-postgresql`, `junit-jupiter` → `testcontainers-junit-jupiter`, plus
`testcontainers-kafka`; the `1.20.4` pin is gone from the root `dependencyManagement` and versions now
come from the Boot BOM. Deferred since the split, it stopped being deferrable the moment a third
testcontainers artifact had to join a knowingly inconsistent classpath.

Two things to know. Packages did not move (`org.testcontainers.containers.GenericContainer` is still
there), so existing imports mostly survived. But `PostgreSQLContainer` is **no longer generic** in
2.x — it declares its own self-type (`extends JdbcDatabaseContainer<PostgreSQLContainer>`), so
`PostgreSQLContainer<?>` no longer compiles.

The migration also produced the third `'dependencies.dependency.version' is missing` of this project,
and from the same cause every time: an artifact is declared that no `dependencyManagement` entry
covers. Here the management entries were renamed while the declarations in **both** modules kept the
old names. The diagnosis is always `help:effective-pom` — if the artifact has no `<version>` there,
the search is over.

### Producer side in the monolith — the slice runs end to end (2026-09-07)

A protocol now travels the whole path: finalised in the UI → Kafka → `published_assignments` →
`GET /pub/api/v1/protocols/{number}`. **Verified by hand on the running compose stack**, all four
transitions: finalise → appears; edit a finalised protocol → updates; un-finalise → disappears;
delete → disappears.

Pieces added: `ProtocolSnapshotMapper`, a `Clock` bean, two domain events, `ProtocolPublicationListener`,
and `sendTombstone` on the producer.

#### Publish after commit, map before it

The snapshot is built **inside** the transaction and sent **after** it commits. Both halves are
forced:

- inside, because `protocolContents`, `tourist`, `grade`, `kindOfTourism` are all `LAZY` — after the
  commit there is no session left to resolve them;
- after, because sending inside the transaction can publish a protocol whose commit then fails.

The joint is an application event carrying the finished `ProtocolSnapshot`, consumed by
`@TransactionalEventListener(phase = AFTER_COMMIT)`. Spring holds the event until the commit and then
runs the listener on the same thread, outside the transaction. A rollback drops it silently.

Events are their own types (`ProtocolPublished`, `ProtocolUnpublished`) rather than `ProtocolSnapshot`
and `int`. Dispatch is by parameter type, so an `int` listener would mean "react to any integer
published anywhere", and reusing the contract DTO as a domain event would make every future
`ProtocolSnapshot` publication send to Kafka by accident.

#### The write path's object graph is not a valid source for the payload

`ProtocolContentMapper.toEntity` maps `tourist.id` from a form field, so MapStruct constructs a
`Tourist` with **only the id set**. Enough for the insert — Hibernate needs the foreign key — but
`getLastName()` on it returns `null`, with no query and no exception.

So `publishProtocolUpdated` re-reads the protocol through
`getProtocolWithContentByIDs`, which `LEFT JOIN FETCH`es both `protocolContents` and `tourist`. One
query; `Grade` and `KindOfTourism` come from the L2 cache (ehcache, `@Cache` on both), so there is no
N+1 — it is 1, not 1+3N.

The general shape worth keeping: **an entity graph assembled for writing carries identifiers, not
data.** Building an outbound payload from it produces a silently empty message.

#### Mapper decisions

- **Hand-written, not MapStruct.** Three fields out of sixteen match by name; the rest are renames
  (`order` → `orderNumber`), dereferences (`grade.title`), embedded-key access (`id.rowNum`) and
  computations. MapStruct's one advantage does not apply, and an explicit constructor call keeps the
  field list auditable — which matters because the payload is the security boundary.
- **`assignmentDate` = `protocol.date`.** There is no such column on `ProtocolContent`; the protocol's
  date is the date the grade was awarded. A semantic decision, not a mapping detail.
- **`expiresInYears == 0` → `validUntil = null`**, per the contract's own javadoc: null means an
  indefinite grade. Without the branch a permanent grade would expire on the day it was issued —
  a correct-looking date, noticed a year later if at all.
- **`publishedAt` comes from an injected `Clock`.** Deriving it from `protocol.date` was tried and
  rejected: it duplicated a field already in the payload, gave identical values to protocols
  published months apart, and did not change on re-publication, so a consumer could not tell which
  snapshot was newer. `Clock` keeps the mapper a pure function and `Clock.fixed` keeps the
  exact-JSON test deterministic. The bean lives in its own `ClockConfig` (it was briefly in
  `WebConfig`, which would drag MVC into every narrow test context) and uses `systemDefaultZone()`,
  because a shared clock will eventually be used for `LocalDate.now(clock)` and UTC would give
  yesterday's date after 03:00 Moscow time.

#### Four cases, and why the previous status is not needed

| Event | Published |
|---|---|
| saved as `FINALIZED` | snapshot |
| saved as `DRAFT` | tombstone |
| deleted | tombstone |

The second row covers un-finalisation, the third was missing at first — a deleted protocol would have
stayed in the public registry forever, which the design doc calls the worst kind of failure.

Nothing reads the old status, because a tombstone for a never-published protocol deletes zero rows.
The same idempotency the whole design rests on removes the need for a read-before-write.

**`number` may be null and that is accepted.** Such a protocol reaches the registry but
`findProtocolByNumber` cannot address it — present and invisible. Recorded as a decision so it is not
mistaken for a defect later.

#### Producer-side contract test

`ProtocolProducerServiceIT` sends a fixed snapshot through the real `ProtocolProducerService` and
reads the raw bytes back, asserting the **whole body** against a JSON literal, the key, and the
absence of a `__TypeId__` header.

The literal is derived from the serialiser's actual output, not written from memory — `Instant` turns
out to be an ISO string rather than an epoch decimal, and field order follows the record's component
order. Asserting the entire body, not selected fields, is what turns the design doc's rule "only the
listed fields cross the boundary" from discipline into mechanics: add a field to `ProtocolSnapshot`
and this test plus the consumer-side literal both go red.

Context is narrow (`@SpringBootTest(classes = {ProtocolProducerServiceImpl.class, KafkaTopicConfig.class})`
plus `@ImportAutoConfiguration(KafkaAutoConfiguration.class)`), so it needs neither a database nor the
Telegram bot mocks, and it goes red only when the producer or its config breaks. `application.yaml`
is still read, so the serialiser settings under test do apply.

#### Blind spot: nothing tests the after-commit path

Every write-calling test in `ProtocolServiceIT` is `@Transactional` and therefore rolls back, so
`AFTER_COMMIT` listeners never fire there. Those tests structurally cannot cover publication.

`@MockitoBean ProtocolProducerService` was added anyway, as a guard rather than a fix: correctness
currently rests on every writing test carrying `@Transactional`, and removing one would silently turn
a test into a real Kafka send blocking for `max.block.ms`.

Covering the path for real needs a non-transactional test with manual cleanup, asserting
`verify(producer).sendProtocol(...)`. Mocking the producer rather than `ApplicationEventPublisher` is
deliberate: mock at the process boundary, not at internal wiring — the publisher is implemented by
the `ApplicationContext` itself, and replacing it would silence every event in the context while
cutting the chain before the interesting part.

#### Incidental fixes

- **`commons-io` pinned to 2.18.0** in `instructors-app`. Three consumers ask for it — telegrambots
  2.15.1, commons-compress 2.16.1, POI 2.18.0 — and nearest-wins gave 2.15.1, so Testcontainers
  crashed with `NoSuchMethodError: FileTimes.toUnixTime`. Pin the **highest** version requested, not
  the lowest that stops the crash: the lowest silently downgrades whoever asked for more and lines up
  the next failure of the same kind.
- **`spring.kafka.admin.auto-create: false`** in the test profile. Adding Kafka to the monolith made
  `KafkaAdmin` try to create topics at every context start, flooding unrelated tests with rebootstrap
  attempts against `localhost:9092`. A reminder that a new dependency changes every test booting the
  full context.
- **nginx: `/api/` → `/pub/`** with a trailing slash on `proxy_pass`, so `/pub/api/v1/protocols/15`
  reaches the service as `/api/v1/protocols/15` and `/api/` stays with the monolith. The trailing
  slash is right *here* precisely because the external prefix differs from the application's path.
- **Local dev data** loads through Flyway: `SPRING_FLYWAY_LOCATIONS` gains a `filesystem:/app/devdata`
  entry in `docker-compose.dev.yml`, which mounts the fixture read-only. The file never enters the war,
  so it cannot leak into production, and it is opted into by the same `COMPOSE_FILE` mechanism as the
  debug ports. Renamed `V999__` → `R__dev_data.sql`: a versioned 999 would make every later real
  migration out-of-order and Flyway would refuse it. Being repeatable, it must be idempotent —
  it deletes before inserting — and it calls `setval` on the sequences, without which explicit ids
  leave the sequence behind and the first record created through the UI collides.

### Transactional outbox — the dual-write hole is closed (2026-09-10)

`ProtocolServiceImpl` now writes a row into `published_protocols_outbox` **in the same transaction as
the protocol**, and a scheduled relay drains it to Kafka. "The protocol is finalised" and "the message
is queued" became one atomic fact, so *finalised but never published* is no longer reachable: if the
process dies between commit and send, the row is still there and the relay picks it up on restart.

The estimate held. Deleted: `ProtocolPublished`, `ProtocolUnpublished`, `ProtocolPublicationListener`
— about ten lines of wiring. Survived untouched: `ProtocolSnapshotMapper`, building the snapshot
inside the transaction, the four publication cases, the topic/key configuration and both contract
tests.

#### The write must be a direct call, not an application event

`@TransactionalEventListener(AFTER_COMMIT)` fires **after** the commit, so a listener writing the
outbox row would open a *new* transaction — restoring exactly the hole being closed, with an extra
layer on top. `BEFORE_COMMIT` would work, but then correctness rests on one word in an annotation:
change it to the more familiar `AFTER_COMMIT` and the guarantee disappears silently.

The decoupling argument does not apply either — `ProtocolServiceImpl` already imports
`ProtocolSnapshot` and calls the mapper. The events existed to defer work past the commit; with an
outbox there is nothing to defer.

#### Payload is serialised at write time and never rebuilt

Storing `protocol_id` and re-deriving the snapshot at send time would be wrong, and the design doc
already says why: between commit and send someone may rename a grade in the catalogue, and the
rebuilt payload would carry the new wording — retroactively rewriting an issued credential.

**Which mapper produces those bytes matters more than it looks.** They are the wire format, pinned
character-for-character by the contract tests. Boot's autoconfigured `ObjectMapper` is tuned for HTTP
and may differ on dates; `JacksonMapperUtils.enhancedJsonMapper()` is the exact mapper spring-kafka
builds for `JacksonJsonSerializer`, so using it removes the question instead of answering it. Proof
that it worked: the serialisation path moved out of the Kafka client entirely and the byte-exact
assertions stayed green.

Consequently the producer's value serialiser went back to `StringSerializer` — the relay sends the
stored string verbatim, with no second pass through Jackson.

#### Producer: synchronous, throwing, one method

`send(String key, @Nullable String payload)` blocks on `.get(5s)` and throws `ProtocolPublishException`.

Returning a `CompletableFuture` was rejected: there is exactly one caller and it always has to wait —
without the result it cannot decide whether to set `sent_at`. Merging producer and relay was rejected
for the opposite reason: one knows the transport, the other the drain policy, and they change for
different reasons.

The pre-outbox `whenComplete` logging had to go, and not only for tidiness: a `void` method that
logs the failure asynchronously **can never report it to the caller**, so a `try/catch` in the relay
would have caught nothing. Tombstones collapsed into the same method — at transport level a tombstone
is just a send with a null value; the two-method split belonged to the domain layer, which no longer
exists here.

#### Relay

`@Scheduled(fixedDelay = 1000)`, single-threaded, `ORDER BY id`, synchronous send per row, `sent_at`
written immediately after each success, `attempts` incremented only on failure, and **on failure the
pass stops rather than skipping to the next row**.

`fixedDelay` rather than `fixedRate` is load-bearing: `fixedRate` can start the next pass before the
previous finished, and two relays running at once break ordering — which is the one property the
whole design rests on, since an older snapshot arriving after a newer one silently overwrites it.

The relay is guarded by `@ConditionalOnProperty("protocols.outbox.relay.enabled", matchIfMissing =
true)` — the property was renamed from `outbox.relay.enabled` when the cleanup landed, see below — and
switched off in the test profile — otherwise every `@SpringBootTest` would drain the outbox
mid-assertion. `matchIfMissing` is deliberately `true`: forgetting to disable it in a test is noticed
in one run, forgetting to enable it in production would silently publish nothing.

**Constraint to remember: the relay assumes a single application instance.** Two instances would
interleave rows and destroy ordering. `FOR UPDATE SKIP LOCKED` is the usual answer but does not
preserve order either. Anyone scaling the monolith horizontally must solve this first.

#### Rejected: skipping rows after N attempts

An `attempts < threshold` filter in the relay's query was tried and removed. It looks like a safety
valve and is in fact silent data loss.

With one-second ticks and a five-second send timeout, ten attempts is about **a minute of Kafka being
unavailable** — less than a container restart. After that the row leaves the query permanently, does
not come back when the broker returns, and needs a manual `UPDATE ... SET attempts = 0`. One ERROR
line, then silence.

The filter also guards against a case that cannot really occur here: the payload is an
already-validated string and `send` fails only for transport reasons, which hit every row equally —
so skipping the head of the queue buys no progress. `attempts` remains, used only to escalate log
level.

#### Two API facts found the hard way

- **pgjdbc does not accept `java.time.Instant`.** Its `PgPreparedStatement` handles `LocalDate`,
  `LocalDateTime`, `LocalTime`, `OffsetDateTime` and `OffsetTime` — `Instant` appears nowhere, and an
  explicit `Types.TIMESTAMP_WITH_TIMEZONE` hint does not help, because the problem is the conversion,
  not the target type. `sent_at` is now set by the database's `now()`, matching `created_at`.
- **A narrow `@SpringBootTest(classes = …)` context cannot host this test.** Besides missing the
  `DataSource`, it excludes the application class — so `@EnableScheduling` never applies and the relay
  bean is created but never ticks. The symptom would be a timeout with nothing in the log.

#### Tests, and two ways they lied

`ProtocolOutboxWriterIT` (3) proves the row is transactional — written inside a `TransactionTemplate`
marked rollback-only, then asserted absent. That test only means something as a **pair** with the
commit case: alone it would pass even if `enqueue` did nothing at all.

`ProtocolPublicationIT` (2) covers the chain end to end — writer → relay → topic — and asserts
ordering of two messages under one key, which is the property the single-threaded synchronous relay
exists to provide.

Both bugs met while writing them are worth remembering:

- The test copied the relay's `WHERE sent_at IS NULL` into its own helper. In the relay that filter
  selects unsent rows; in the test it inverted the assertion — asking for rows *not yet sent* and then
  requiring that they *had* been sent. **Do not copy queries out of the code under test**: the test
  then verifies that a copy matches the original rather than that the system behaves.
- Both tests used the same message key. The outbox table can be truncated between tests, but the Kafka
  topic cannot — a fresh consumer group with `earliest` reads the previous test's messages too, and
  `findFirst()` by key picks whichever came first. Distinct keys per test, not just a clean table.

### Outbox retention cleanup (2026-09-24)

`ProtocolOutboxCleaner` deletes sent outbox rows older than a configurable window, on a cron schedule.
`CleanUpProperties` (`@ConfigurationProperties("protocols.outbox")`) carries `retention-days`; the cron
expression is read straight from `${protocols.outbox.cleanup-cron}` in `@Scheduled`.

```sql
DELETE FROM instructors_grades.published_protocols_outbox
WHERE sent_at IS NOT NULL
  AND sent_at < now() - make_interval(days => :retention_days)
```

#### Decisions

- **Only rows with a non-null `sent_at` are removed.** Rows still unsent — including the poison ones
  the relay abandoned past `ATTEMPTS_THRESHOLD` — are never deleted here. They are the only record that
  something failed, and nothing yet moves them to a dead-letter table or alerts on their count. That is
  the remaining hole in the outbox lifecycle, promoted to Next steps.
- **The boundary is exclusive.** `<`, not `<=`, so a row exactly `retention-days` old survives one more
  run. Both sides of that edge are covered by a test.
- **No index for this predicate, on purpose.** `idx_outbox_unsent` is partial on `sent_at IS NULL` and
  cannot serve the cleanup, so the nightly delete is a sequential scan. An index on `sent_at` would be
  paid on every outbox insert to speed up one statement a day.
- **The deleted row count is logged.** `update()` returns it; without that line there is no way to tell
  a working cleanup from one that silently matches nothing.
- **Bad config fails at startup, not at 01:00.** `@Validated` + `@Positive` on `retentionDays` means
  `retention-days: 0` aborts the context. A plain `int` with no validation would bind to `0` and the
  first run would delete every sent row in the table.
- **`cleanup-cron` is deliberately not a component of `CleanUpProperties`.** `@Scheduled` resolves
  placeholders itself; binding the same key a second time into a field nobody reads is one more place to
  forget when the key is renamed. The cost is recorded under metadata below.
- **The relay and the cleaner now share one prefix.** `@ConditionalOnProperty` on `ProtocolOutboxRelay`
  moved from `outbox.relay.enabled` to `protocols.outbox.relay.enabled`, with `application-test.yaml`
  and `ProtocolPublicationIT` following. Two config roots for one subsystem was a rename waiting to
  bite.

#### `@ConfigurationProperties` on a record does not survive `@Validated`

Three failures in a row, each with a different cause:

1. **`@Validated` on the record component instead of the type.** It compiles — the annotation targets
   `METHOD` and `PARAMETER`, so it lands on the accessor and the constructor parameter. But Boot looks
   for it on the **type**: `ConfigurationPropertiesBinder` adds the JSR-303 validator only when
   `target.getAnnotation(Validated.class) != null`, and `Bindable` carries type annotations. Validation
   was simply off, with nothing to show for it.
2. **`@Validated` on the type, with a record.** `Cannot subclass final class CleanUpProperties`. Method
   validation wants an AOP proxy, `spring.aop.proxy-target-class=true` selects CGLIB, CGLIB proxies by
   subclassing, records are `final`. JDK proxies are no escape — a record implements no interface.
3. So `CleanUpProperties` became an ordinary class (`@Getter` + `@RequiredArgsConstructor`; a single
   parameterised constructor, so Boot still uses constructor binding). The alternative that keeps the
   record is a check in the compact constructor: the binder calls the canonical constructor, so it
   fails at startup all the same, with no AOP involved.

A fourth failure came from mixing mechanisms — `@Value("retention-days")` left on the field next to
`@ConfigurationProperties`. `@Value` without `${…}` is a literal, and field injection wins the race:
`Failed to convert value of type 'java.lang.String' to required type 'int'; For input string:
"retention-days"`. The two are competing ways to fill the same field; pick one.

#### Tests, and a negative test that passed for the wrong reason

`ProtocolOutboxCleanerIT` (2) covers the SQL against a real Postgres: one method for the mixed
population (old sent / never sent / just sent), one for the boundary at ±1 minute. Kept as two
multi-assertion methods on purpose — the combination of states in one table is what production looks
like.

The first version of the mixed-population test had three rows but only **two** states: two of them had
`sent_at IS NULL`. An implementation that dropped the age condition and deleted everything with a
non-null `sent_at` would have passed it. The "sent, but recently" row is what makes the test mean
anything.

`ProtocolOutboxCleanerTest` (2) checks the properties binding with `ApplicationContextRunner`, no
container: `retention-days=0` must abort the context, `retention-days=1` must bind to `1`. Two traps on
the way:

- `withUserConfiguration(CleanUpProperties.class)` registers the class as an **ordinary bean**. The
  `@ConfigurationProperties` infrastructure is absent from a bare runner, so Spring autowired the
  constructor instead of binding it: `No qualifying bean of type 'int' available`. Both tests failed
  identically — and the negative one **reported success**, because `hasFailed()` was satisfied by the
  wrong failure. A negative test is only trustworthy next to a positive one. Fixed with a nested
  `@EnableConfigurationProperties(CleanUpProperties.class)` configuration class.
- `hasMessageContaining` inspects only the top exception's own message. The field name lives two levels
  down in `BindValidationException`; the top `ConfigurationPropertiesBindException` says nothing but
  "Could not bind properties … prefix=protocols.outbox". Assert through `rootCause()` (or
  `hasStackTraceContaining`) and pin the exception type, not just a substring.

Testcontainers 2.0 detail worth recording: `org.testcontainers.postgresql.PostgreSQLContainer` is
**not** generic — the self-type is fixed inside. The generic `PostgreSQLContainer<SELF>` is the older
`org.testcontainers.containers` one.

#### `spring-boot-configuration-processor` was on the classpath and doing nothing

Adding it to `<dependencies>` is the usual recipe and here it was inert: once
`<annotationProcessorPaths>` is configured, the plugin passes javac an explicit `-processorpath`, which
**disables processor discovery on the classpath entirely**. The list becomes closed — MapStruct, Lombok,
their binding, and nothing else. No error, no warning, just no metadata.

It also needs an explicit `<version>`: paths in that block are resolved by the plugin's own code, which
in `maven-compiler-plugin` 3.11.0 does not consult `dependencyManagement` — hence `version can neither
be null, empty nor blank`, and hence the explicit versions already sitting on the Lombok and MapStruct
entries. `${project.parent.version}` is a trap here: the block is inherited, so in `instructors-app` it
resolves to the aggregator's `0.0.1-SNAPSHOT` rather than Boot's version. A `spring-boot.version`
property in the root pom is the plain fix. From 3.13.0 there is an opt-in
`annotationProcessorPathsUseDepMgmt` (default `false`) that would remove the duplication.

`target/classes/META-INF/spring-configuration-metadata.json` is now generated and holds
`protocols.outbox.retention-days`. It does **not** hold `cleanup-cron`, and cannot: placeholder keys
never reach the binder, so the processor has nothing to describe. The IDE will keep flagging that one
key as unresolved — expected, not a defect.

#### Verification

The formatting debt on `ProtocolPublicationIT.java` recorded earlier is paid — `spotless:check` is green
across every module. Committed as `260ba4c`, `d8d9f0b`, `1b255ee`, `ff0653d`.

### Poison rows: the relay no longer blocks the queue (2026-09-25)

Option B from the dead-letter discussion: a `dead_at` marker in the outbox table rather than a separate
DLQ table. Migration `V7` adds `dead_at timestamptz` and `error_message`, `readRows` filters on
`dead_at IS NULL`, and the retention cleaner needs no change — its `sent_at IS NOT NULL` never matched
those rows anyway.

#### The bug this fixes was worse than "rows accumulate"

`ATTEMPTS_THRESHOLD` only ever escalated the log level. It touched neither the query nor the control
flow, and the loop did `break` on the first failure. So a row that could **never** be sent stayed first
by `id` for ever, and everything behind it was never published either — head-of-line blocking, not
merely a growing table.

The reasoning recorded above for that `break` ("`send` fails only for transport reasons, which hit every
row equally") was right about transport and incomplete about everything else. `RecordTooLargeException`
— a protocol with enough assignments to exceed `max.request.size` — is permanent, row-specific, and
reachable. So are authorization errors and `InvalidTopicException`.

#### Deadness is decided by exception type, not by an attempt count

The first attempt at a fix used `attempts >= ATTEMPTS_THRESHOLD` as the death signal. That **loses
data**: there is no backoff, `fixedDelay` is one second, so a 15-second broker restart burns the whole
ten-attempt budget on a perfectly healthy row and buries it. It traded a recoverable stall for
unrecoverable loss — the wrong direction for a service whose entire point is that publications do not
vanish.

`attempts` cannot distinguish "the broker is down" from "this row is poison". Only the exception can.
`ProtocolPublishException` is now `abstract` with two subclasses, and `ProtocolProducerServiceImpl`
decides between them on `e.getCause() instanceof RetriableException` — Kafka's own marker interface for
what is worth repeating. The classification lives in the producer because that is the only class that
should know Kafka internals; the relay reads two `catch` blocks and nothing else.

`ATTEMPTS_THRESHOLD` is back to its original job: log level only. "The broker has been down for three
days" is a question for monitoring, not for a counter that deletes work.

#### `break` belongs to the retryable branch

Getting these backwards costs asymmetrically, and it was gotten backwards once:

- **retryable** means every remaining row will fail identically → `break`. Without it a broker outage
  walks all 100 rows of the batch, each blocking on `.get(5, SECONDS)`, turning one tick into ~500
  seconds and logging all 100 at ERROR once they cross the threshold.
- **permanent** means only this row is bad → carry on. A dead row should cost the rest of its tick,
  nothing more.

A second reason for `break` on retryable: `InterruptedException` re-sets the interrupt flag, so during
shutdown every later `.get()` in the same tick throws instantly and would otherwise run the counter up
on the entire batch in microseconds.

#### Tests

`ProtocolOutboxRelayIT` (3), and the shape matters: **each case needs two rows in the table**. With one
row `break` and its absence look identical — a single-row test would have passed against the exact bug
being fixed. The cases are permanent-then-next-row-sent, retryable-then-next-row-untouched, and
dead-row-not-selected-at-all.

The relay is built by hand (`new ProtocolOutboxRelay(producer, jdbcClient)`) rather than taken from the
context. Enabling the bean would also start its one-second schedule, and the background ticks race the
test; constructing it directly keeps `relay.enabled=false` intact and the test deterministic.

#### Two build debts closed, one lesson about where they hide

The Mockito javaagent was configured for surefire in `instructors-app` only. `ProtocolOutboxRelayIT` was
the project's first mock under **failsafe**, which surfaced the other half of the landmine recorded
below. Both plugins in both modules now carry the agent, and `mockito-core` is declared explicitly
instead of arriving through `spring-boot-starter-test`.

The mechanism is worth writing down because it looks like magic and is not: `${groupId:artifactId:type}`
is **not** a surefire feature. It is an ordinary Maven property, colons and all, set by
`maven-dependency-plugin`'s `properties` goal. In the root pom that plugin sits in `<pluginManagement>`,
which configures but does **not activate** — `instructors-app` listed it under `<build><plugins>`,
`publication-service` did not. So the placeholder reached the JVM verbatim there and the fork died with
`Error occurred during initialization of VM`. Same cause made jacoco inert in that module: no `.exec`
file was ever produced, and the 50% threshold everyone assumed was guarding it was guarding nothing.

Both plugins are now activated in `publication-service`. Coverage is measured there for the first time
and **passes** on the tests that were already written.

A smaller lesson: `ProtocolOutboxRelayIT` shipped with `import static org.junit.jupiter.api.Assertions.*`
and nothing objected. Spotless does not expand star imports, and checkstyle's `AvoidStarImport` never
looked at the file — `includeTestSourceDirectory` is still unset. That deferred debt has a concrete cost
now, not a hypothetical one.

### The coverage gate was green and measuring nothing (2026-09-26)

`jacoco:check` had been reporting "All coverage checks have been met" for as long as anyone had looked,
and it was not checking this build. Both `check` and `merge-results` were bound to `verify`, and within
one phase the order comes from the order of declaration in the POM — `check` was declared first. So it
read a `jacoco-merged.exec` that the *previous* build had left behind.

Two ways that goes wrong, and both had been happening:

- on a build without `clean`, the gate judges stale data. That is how it passed while
  `publication-service` was reported at `0.13` one run later: the file it had approved was a different
  file from the one the failing run produced;
- on `mvn clean verify` there is no merged file at all when `check` runs, and jacoco logs
  `Skipping JaCoCo execution due to missing execution data file.` at **INFO** and passes. A gate that
  silently abstains looks exactly like a gate that agrees.

Fixed by expressing the dependency through phases rather than through line order: `merge-results` on
`post-integration-test`, `report-merged` and `check` on `verify`. Merging is now guaranteed to precede
both by the lifecycle, not by where the XML block happens to sit.

The same mistake bit `report-merged` on the way — it was declared above `merge-results` while both sat
on `post-integration-test`, so the merged HTML report was skipped and `target/site/jacoco-merged` never
appeared. Two rounds of the identical error is the signal worth keeping: **an ordering constraint
expressed as XML sequence is not expressed at all.**

First numbers ever produced by the gate on data from its own build:

| Module | Instructions covered | Total | Ratio |
|--------|---------------------|-------|-------|
| `instructors-app` | 6750 | 7679 | 87.9% |
| `publication-service` | 515 | 566 | 91.0% |

The 50% threshold clears comfortably in both. Worth noting what this vindicates: the stance recorded
above — write tests from the first class rather than waive the threshold — was never actually enforced by
the build. Had the threshold been "temporarily" lowered when it looked inconvenient, nobody would have
found out that it did not need to be.

### Second-level cache was configured and switched off by omission (2026-09-26)

`ehcache.xml` declared all three regions (`Grade` and `KindOfTourism` with a 1-hour TTL, `User` with a
30-minute TTI, 100 entries each) and Hibernate never read it. `hibernate.javax.cache.provider`,
`use_second_level_cache` and `region.factory_class` were set; `hibernate.javax.cache.uri` was not. Without
it `EhcacheCachingProvider` builds a CacheManager from its own default —
`urn:X-ehcache:jsr107-default-config`, which declares no caches — so Hibernate asked for each region,
found nothing, and the provider created it with provider defaults: **unbounded heap, no expiry**.

That is what the three `HHH90001006` warnings were saying. The usual reading of that warning is "you
forgot to declare the region"; here it was the opposite, and the consequence was real rather than
cosmetic. `User` in particular would have been cached for the lifetime of the process.

One detail worth keeping: the value is `ehcache.xml`, **not** `classpath:ehcache.xml`. Hibernate passes
the string through to JCache, which resolves it as a resource name; the `classpath:` form fails with
`Couldn't load URI from classpath:ehcache.xml` because no such URL protocol exists. Verification that it
took effect is two lines in the log — zero `HHH90001006`, and `CacheManager=file:/…/ehcache.xml` instead
of the `urn:X-ehcache` default.

`spring.jpa.open-in-view` is now explicitly `false`. It was on by default, which kept the EntityManager
open through view rendering — lazy loads from templates, connections held for the length of HTML
generation, and queries running outside any transaction. It turned out to cost nothing here: controllers
hand DTOs to Thymeleaf rather than entities, so no lazy access happened during rendering and the full
test suite stayed green.

While checking that, one more documentation defect surfaced: the stack list claimed "Spring Cache +
Ehcache". There is no Spring Cache in the project — no `@EnableCaching`, no `@Cacheable`, no
`CacheManager` bean, no `spring.cache.*`. Ehcache serves only as Hibernate's second-level provider.
README corrected.

## Not started

Observability. Neither module has `actuator` or `micrometer` — for two processes joined by a queue that
means consumer lag, outbox depth and a non-empty DLT are invisible except by reading logs. This moved
from "nice to have" to "needed" the moment rows started being marked dead: nothing removes them, and
nothing watches them.

## Next steps

**The split as architecture is finished.** Every mechanism in the design doc is implemented and under
test: separate database and Flyway, state transfer with the protocol id as message key, tombstones,
compaction, the transactional outbox, an idempotent consumer, the public read endpoint, retention, and
now dead-row handling. There is no "remove it from the monolith" stage — all nineteen monolith
controllers are internal, so the registry was added rather than moved. What remains is hardening and the
public-facing authentication.

`mvn clean verify` on the whole reactor is green, with no model warnings, no Mockito self-attach warning
and no skipped jacoco step anywhere. Coverage clears the 50% threshold in both modules on freshly merged
data — 87.9% and 91.0%, see the coverage-gate section above:

```
instructors-app       surefire 342      failsafe 23
publication-service   surefire   8      failsafe   8      → 381 tests
```

Ordered by what would hurt most if left alone:

1. **Observability.** `actuator` + `micrometer`, then the three signals that matter: consumer lag, a
   non-empty DLT (always an incident), and outbox rows with `sent_at IS NULL` older than ~15 minutes.
   That last one is deliberately time-based: `attempts` crosses any threshold within seconds of a brief
   broker hiccup, so it cannot tell an outage from a stuck row. Do this **before** authentication —
   while there are no external clients the cost of being blind is zero, and on the day the first one
   arrives it is at its highest. Dead rows in particular are now produced, never cleaned, and watched by
   nobody.
2. **Authentication on the public API.** API keys in a header first, per the design doc: they give a
   clear model of who the client is, what it may do and how to revoke it. Keys live in the service's own
   database — reading the monolith's `User` table is exactly the coupling the split removed. OAuth2
   client credentials earn their complexity once there are several clients.
3. **Decide how durable the topic really is.** The design argues that a compacted topic makes the
   service database a cache that can be dropped and rebuilt, which is what excuses it from backups. But
   `replicas(1)` on a single broker means that source of truth lives on one disk with no copy. Either
   run RF ≥ 3 with `min.insync.replicas=2` in production, or accept that the service database needs
   backing up after all. Replication factor, unlike partition count, can still be changed later — a
   reassignment is tedious but does not break per-key ordering.
4. **Deferred debts.** Set `includeTestSourceDirectory` so checkstyle reaches test sources — this one
   has already cost something, see the star import above. Then: drop the dead `?currentschema=` from the
   jdbc urls; apply `bind: { create_host_path: false }` to the single-file compose mounts; add
   `additional-spring-configuration-metadata.json` for `cleanup-cron` if the unresolved-key warning in
   the IDE starts to annoy. A dedicated DLQ table remains the fuller answer to dead rows, worth building
   when the first real one appears and not before — it needs its own retention policy, which is the
   opposite of the outbox's: dead rows are incidents, not noise.

### Checkstyle in `publication-service`: the feared breakage did not happen

Run on its own, `checkstyle:check` reports `0 Checkstyle violations` and succeeds. The worry recorded
here and in `CLAUDE.md` — that `<configLocation>checkstyle.xml</configLocation>` resolves relative to
each module and would break in a new one — **did not materialise**; the plugin finds the config. It
was invisible until now only because the build died at spotless first.

One caveat: `includeTestSourceDirectory` is not set, so checkstyle scans `src/main` only. That is why
the dead `Streams` import in the integration test survived even though `UnusedImports` is enabled —
the rule exists but is not pointed at test sources.

### Landmine defused

The surefire `-javaagent:${org.mockito:mockito-core:jar}` argLine moved out of the shared
`pluginManagement` into `instructors-app`'s own `<build><plugins>`, where mockito actually exists.
Verified through `help:effective-pom`: `publication-service` now inherits surefire with **no**
configuration at all, so its first test ran instead of failing on an unresolvable property.

Mockito still self-attaches there with a warning (it arrives via `spring-boot-starter-jdbc-test`).
Harmless today, but when that module gets its first mock the javaagent will have to be configured
locally — this time with the mockito dependency to go with it.

**Fully defused 2026-09-25.** Both modules now configure the agent on surefire *and* failsafe, and both
declare `mockito-core`. Two corrections to the account above. First, failsafe needed it in
`publication-service` too, even though no integration test there mentions Mockito: every `@SpringBootTest`
drags in `MockitoTestExecutionListener`, which initialises the inline mock maker whether or not mocks
were declared. Second, moving the argLine into a module is not enough on its own —
`maven-dependency-plugin` has to be activated in that module as well, or `${org.mockito:mockito-core:jar}`
never becomes a property and reaches the JVM as a literal.

## Decide before writing code

- ~~The read side needs an agreed url and response shape.~~ Decided — see `registry` above.
- ~~Jacoco's inherited 50% threshold will bite as soon as the new module has its first class with
  logic.~~ Settled 2026-09-25, and not the way it was expected to be. The threshold never bit because
  jacoco was never *active* in `publication-service`: the plugin sat in the root `<pluginManagement>`
  and only `instructors-app` listed it under `<build><plugins>`, so not one `.exec` file was produced
  there. Activating it was still not enough — see the coverage-gate section below for the second half
  of the story. The agreed stance paid off on its own in the end: the tests written from the first class
  clear the threshold with nothing waived.
- ~~Migrate testcontainers to 2.x.~~ Done — see above.
- How durable the topic has to be — RF ≥ 3 with `min.insync.replicas=2`, or backups for the service
  database. See Next steps; the design's "the topic is the backup" argument does not hold at
  `replicas(1)`.
- Deferred cleanups, none urgent: drop the dead `?currentschema=` from the jdbc urls; apply
  `bind: { create_host_path: false }` to the single-file mounts; set `includeTestSourceDirectory` on
  checkstyle so its rules reach test sources. ~~Declare Mockito explicitly in `instructors-app`.~~ Done
  2026-09-25.
