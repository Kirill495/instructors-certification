# Multi-Module Build Conventions

The repository became multi-module on 2026-08-17. These conventions are what keep the module
boundary real rather than decorative — several of them were learned by breaking the build.

## Layout

```
instructors/                  parent, packaging=pom
├── instructors-app/          the former monolith (war)
├── publication-service/      the new service (jar)
└── publication-contract/     Kafka payload records, shared by both
```

Infrastructure — `docker-compose.yml`, `nginx/`, `db/`, `config/` — stays at the root. It is shared
by both services.

The root pom inherits from `spring-boot-starter-parent` and the modules inherit from the root, so
Boot's dependency management reaches every module through a single chain. A module has exactly one
`<parent>`, which is why the chain rather than a BOM import.

## Only `*Management` in the root pom

`<dependencies>` and `<plugins>` in a parent are **inherited unconditionally by every module**.
`<dependencyManagement>` and `<pluginManagement>` only say "if you use this, here is the version and
configuration" — the module still has to declare it.

The root pom originally used the first form, which meant an empty `publication-service` carried the
monolith's Telegram bot, Thymeleaf, POI, Envers and ehcache, and inherited
`spring-boot-maven-plugin` (whose `repackage` then failed with `Unable to find main class`). The
modules were coupled by the build before a single line of service code existed.

So: the root answers "which version, configured how", and each module answers "what do I need".

### Do not copy BOM-managed entries into `dependencyManagement`

A local `dependencyManagement` entry **shadows the inherited one wholesale** — the version is not
merged in from the BOM. Copying `spring-boot-starter-data-jpa` there without a version leaves it
with no version at all, and the project model cannot be built.

The root manages only what this project versions itself: `telegrambots`, `mapstruct`, `poi-ooxml`
and the testcontainers pin. Everything else comes from Boot's BOM, as it always did.

### `pluginManagement` holds executions too

Executions belong in the root `pluginManagement` alongside version and configuration. A module then
declares four lines — `groupId` and `artifactId` — and inherits all of it. Executions merge by
`<id>`, so a module can override one without restating the rest.

Only spotless and checkstyle stay in the root `<plugins>`: those are meant to run everywhere, and
inheritance works in our favour there.

### `pluginManagement` also configures plugins nobody declared

Plugins bound by the packaging's default lifecycle — compiler, surefire, jar — pick up
`pluginManagement` configuration even in modules that never declare them.

This is why surefire's `-javaagent:${org.mockito:mockito-core:jar}` argLine belongs in
`instructors-app`'s own pom and not in shared `pluginManagement`. The property is set by
`maven-dependency-plugin`'s `properties` goal only where mockito is actually a dependency;
elsewhere the placeholder reaches the JVM unresolved and tests fail at startup.

## `publication-contract` stays dependency-free

Only records describing the Kafka payload. No JPA, no Spring, ideally not even Jackson annotations.

A shared jar between services is a compromise: it couples them at build time, which is what
microservices are supposed to avoid — a real distributed setup would share a schema in a registry
instead. For a single developer the compile-time safety is worth it, but only under that rule.
Shared modules metastasize: first a DTO, then a helper, then a constant, and within a year it is a
library through which everything is coupled again. If both services need something that is not the
exchange contract, it gets duplicated.

## Two different `db/`

- **`db/` at the repository root** is the Postgres image build context — `Dockerfile`, `init.sh`,
  role and schema bootstrap run once at container initialisation. Shared infrastructure; it stays at
  the root and will grow a second user and database for the publication service.
- **`db/` inside `src/main/resources/db/migration`** is a classpath prefix, part of the Flyway
  convention (`classpath:db/migration`). It has nothing to do with the root directory; the names
  merely collide.

Dropping that prefix during the module move made Flyway silently find no migrations. Keep
`fail-on-missing-locations: true` so it fails loudly instead.

## Verifying the build, cheapest first

1. `mvn validate` — reads the model of every module and stops. Catches missing versions, unreadable
   modules, unresolved properties. Seconds, no compilation.
2. `mvn help:effective-pom -pl <module>` — the pom **after** inheritance, BOM import and property
   substitution. This is the ground truth for whether the boundary came out as intended; never
   reason about Maven's merge rules instead of reading this. Also available in IntelliJ's Maven
   panel as "Show Effective POM".
3. `mvn dependency:tree -pl <module>` — what actually landed on the classpath, transitives included.
4. `mvn clean install` — the full run.

Always run from the repository root. Running Maven inside a module directory silently resolves the
sibling modules from `~/.m2` instead of the reactor, so an edited contract compiles against the
previously installed jar — no error, wrong result. Use `mvn -pl <module> -am` from the root to build
one module together with its dependencies.

## Known debt

Testcontainers is pinned to `1.20.4`. Boot 4 brings `testcontainers-bom` 2.0.3, where the artifacts
were renamed (`postgresql` → `testcontainers-postgresql`, `junit-jupiter` →
`testcontainers-junit-jupiter`). Migrating is a major-version upgrade with API changes and belongs
in its own task, not mixed into structural work.
