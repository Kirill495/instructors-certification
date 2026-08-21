# Project Status

Current state of the monolith-to-services split and the ordered plan for the next work session.

Related: [Publication service design](publication-service-design.md),
[Multi-module conventions](multi-module-conventions.md).

**Last updated**: 2026-08-21

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

### `publication-service` — skeleton in progress

Written: pom with dependencies, `PublicationServiceApplication`, `application.yaml` (partial),
`V1__initial_schema.sql`, a Dockerfile. No `ingest` or `registry` code yet.

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

## Not started

No Kafka anywhere. No `ingest` or `registry` code. Nothing on the producer side of the monolith.
`publication-service` is not in `docker-compose.yml` yet.

## Next steps

1. Commit the compose rewiring: `docker-compose.yml`, `instructors-app/pom.xml`, the deletion of
   `app/`. One commit — it is one logical change, "the monolith build moved into its module".
2. Finish `publication-service/application.yaml` (datasource), add the second Postgres database to
   compose, and get the service to start and create `published_assignments` — again verified by
   watching Flyway apply `V1` to an empty database.
3. Only then: Kafka in compose, `ingest`, `registry`.

### Landmine still armed

`publication-service` declares only `spring-boot-maven-plugin` in its `<build>`. Surefire runs
anyway, driven by the lifecycle, and still picks up the root `pluginManagement` configuration —
including `-javaagent:${org.mockito:mockito-core:jar}`. Nothing sets that property there: the module
declares neither `maven-dependency-plugin` nor mockito. It will detonate on the **first test** in the
module, with an unhelpful "cannot start JVM" message.

The cleaner fix is to move the surefire configuration out of the shared `pluginManagement` and into
`instructors-app`'s own `<build><plugins>`, where mockito actually exists — rather than adding a
dependency to `publication-service` purely to satisfy someone else's setting. Do it before writing
the first test, not after.

## Plan for the next session

Frame the work as **one thin vertical slice**, not "finish the service": get a single protocol to
travel monolith → Kafka → consumer → one HTTP response, crudely, before deepening any layer.
Building the service in full first means designing its storage blind and bending the contract to
fit afterwards.

1. ~~**`publication-contract` first.**~~ Done — see above.
2. **`publication-service` skeleton.** Main class, Postgres, its own Flyway schema and history
   table, one read-model table. This also resolves the `Unable to find main class` that
   `spring-boot-maven-plugin` raises on the currently empty module.
3. **Kafka consumer.** Delete by key, then insert, in one DB transaction; ack the offset only after
   the commit.
4. **Publish from the monolith**, deliberately naive at this point (direct send on finalization).
   The transactional outbox comes after the slice runs end to end.
5. **One `GET` endpoint**, no auth yet.

Out of scope for that session: authentication, transactional outbox, tombstone handling, error
handling.

## Decide before writing code

- Create the topic with `cleanup.policy=compact` from the start. The design already depends on it,
  and changing the policy on a live topic is painful.
- Kafka and the second Postgres database have to go into `docker-compose.yml`, or the slice cannot
  run at all.
- Jacoco's inherited 50% threshold will bite as soon as the new module has its first class with
  logic. Agreed stance: write tests from the first class rather than exempting the module — a
  threshold that gets waived stops meaning anything.
