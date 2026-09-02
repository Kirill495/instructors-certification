# Project Status

Current state of the monolith-to-services split and the ordered plan for the next work session.

Related: [Publication service design](publication-service-design.md),
[Multi-module conventions](multi-module-conventions.md).

**Last updated**: 2026-09-02

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

## Not started

Nothing publishes to the topic yet. The producer side of the monolith is **in progress and known
broken**: `ProtocolProducerServiceImpl` sends a `ProtocolSnapshot` through a `KafkaTemplate<String,
ProtocolSnapshot>`, while `instructors-app/application.yaml` still declares
`value-serializer: StringSerializer`. Generics erase, so this compiles and fails at runtime with a
`ClassCastException` inside the serialiser. It needs `JacksonJsonSerializer`, mirroring the consumer
side above.

No tests for `ingest` at all — everything above was verified by hand against the running stack.

## Next steps

Both ends of the storage exist now: a protocol travels Kafka → storage → HTTP. What is missing is a
producer that puts anything into Kafka, and any automated proof that the write side works.

1. **`mvn spotless:apply` on `publication-service`.** Currently red — `DltProducerConfig.java` has
   format violations. Spotless is *not* missing from the module: it lives in the root `<plugins>`,
   which every module inherits unconditionally, unlike `pluginManagement`.
   Checkstyle has still never run against this module — the build dies at spotless first. Expect
   `<configLocation>checkstyle.xml</configLocation>` to break there, since the path is resolved
   relative to each module. That debt and its fix are recorded in `CLAUDE.md`.
2. **Commit** the `ingest` work.
3. **Tests for `ingest`.** Everything so far was checked by hand with a console producer, which does
   not survive the next refactor. The cases that actually catch things, in order of value:
   a tombstone leaves nothing behind; a smaller snapshot applied over a larger one leaves no orphan
   rows; a neighbouring protocol is untouched in every case; an unknown `version` throws *and writes
   nothing* (the second assertion is the one that proves the check runs before the service call);
   a poison message does **not** look like a tombstone and must not delete anything; dates survive a
   round trip. Applying the same snapshot twice mostly documents intent.
   The risky part is the SQL and the transaction boundary, so most of this belongs on
   `ProtocolIngestService` under `@JdbcTest` with Testcontainers — no Kafka. One integration test
   with a Kafka container covers the wiring and deserialisation. In a test the unknown-version case
   is safe: its own group, its own container, no partition anyone cares about.
4. **Fix and finish the producer in the monolith** — see *Not started* above for the broken
   serialiser. Deliberately naive to begin with: a direct `KafkaTemplate.send` on protocol
   finalisation. The protocol id must go into the **record key**, not just the body — compaction
   works per key and ignores null-keyed records entirely. The transactional outbox comes only after
   the slice runs end to end; adding it now would mean debugging two new mechanisms at once.
   Decide and write down which protocol statuses are publishable: `number` is nullable in the
   monolith, and a draft without a number would produce a url that cannot be addressed.

Out of scope for the slice: authentication, the outbox, tombstone *emission* (the consumer handles
them; nothing produces them yet).

### Landmine defused

The surefire `-javaagent:${org.mockito:mockito-core:jar}` argLine moved out of the shared
`pluginManagement` into `instructors-app`'s own `<build><plugins>`, where mockito actually exists.
Verified through `help:effective-pom`: `publication-service` now inherits surefire with **no**
configuration at all, so its first test ran instead of failing on an unresolvable property.

Mockito still self-attaches there with a warning (it arrives via `spring-boot-starter-jdbc-test`).
Harmless today, but when that module gets its first mock the javaagent will have to be configured
locally — this time with the mockito dependency to go with it.

### Testcontainers version mix is real, not hypothetical

A test run logs `Testcontainers version: 2.0.3` while the root pom pins `org.testcontainers:postgresql`
to 1.20.4. Both are true: 2.x renamed the artifacts (`postgresql` → `testcontainers-postgresql`), so
the pin no longer shadows what the Boot BOM manages, and the classpath ends up with 1.x modules on a
2.x core. It works for now. The migration stays its own task.

## Decide before writing code

- ~~The read side needs an agreed url and response shape.~~ Decided — see `registry` above.
- Jacoco's inherited 50% threshold will bite as soon as the new module has its first class with
  logic. Agreed stance: write tests from the first class rather than exempting the module — a
  threshold that gets waived stops meaning anything. See the surefire landmine above: it detonates
  on that same first test.
- Deferred cleanups, none urgent: drop the dead `?currentschema=` from the jdbc urls; apply
  `bind: { create_host_path: false }` to the single-file mounts; migrate testcontainers to 2.x.
