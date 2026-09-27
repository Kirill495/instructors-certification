# Observability plan

The ordered plan for making both services observable. Item 1 of Next steps in
[STATUS.md](STATUS.md), split out because it is large enough to have its own decisions.

Related: [Publication service design](publication-service-design.md),
[Multi-module conventions](multi-module-conventions.md).

**Created**: 2026-09-27

## Why this comes before authentication

While there are no external clients, the cost of being blind is zero. On the day the first one
arrives it is at its highest. Nothing about the plan below gets cheaper by waiting, and two of the
three signals guard mechanisms that already exist and are already unwatched:

- **Dead rows** (`dead_at IS NOT NULL`) are now produced, never cleaned, and watched by nobody.
- **The DLT** is written to by `DeadLetterPublishingRecoverer` and read by no one.

## Starting point

Committed already: `spring-boot-starter-actuator` and `micrometer-registry-prometheus` in
`instructors-app` and `publication-service`, no version, four lines each. Nothing in the root pom and
nothing in `publication-contract` — see the reasoning in
[Multi-module conventions](multi-module-conventions.md): the root answers "which version", the module
answers "what do I need", and actuator's version is answered by Boot's BOM, so the root has nothing
to say about it.

`micrometer-registry-prometheus` pulls `micrometer-core` transitively, so it is not declared
separately.

Absent, and therefore requiring the first build to run **without** `-o`: both artifacts. The local
repository holds `spring-boot-starter-actuator:3.5.9` and `micrometer-core:1.15.7` from an unrelated
project; the Boot 4 line needs Micrometer 1.16.x.

## Decisions taken up front

| Question | Answer | Why |
|---|---|---|
| Separate management port, or filter endpoint paths? | `management.server.port` | Structural, not a rule to remember. nginx cannot route to a port it does not know. |
| Prometheus container, or just the endpoint? | Container | Lag and counter deltas are time series; a single snapshot cannot tell "catching up" from "diverging". |
| Grafana? | Not yet | A second container and an evening of provisioning for zero new knowledge about the system. Revisit past ~10 signals. |
| DLT: counter or topic lag? | Counter | It records the **event**. Lag records **state**, and anyone draining the DLT erases the evidence. |
| Outbox gauges: query per scrape, or cached? | Per scrape | One small table, four queries a minute. Caching is the pattern to reach for when the query is expensive, not here. |

## Stage 0 — measure the starting point

Build once **online**, start the monolith, then look:

```
curl localhost:8080/actuator/prometheus | grep -c "^[a-z]"
curl localhost:8080/actuator/prometheus | grep kafka
```

The point is not to configure anything. Adding a `MeterRegistry` to the classpath activates a dozen
auto-configurations — JVM, Hikari, Tomcat, `http.server.requests`, Hibernate — and that inventory has
to be seen before a single metric is written by hand. Otherwise the first thing written duplicates
something that was already there.

**Trap.** Only `health` is exposed over HTTP by default. `/actuator/prometheus` answers `404` and it
looks as though the registry failed to load. `management.endpoints.web.exposure.include` fixes it.

**Verify, do not assume**: that the endpoint id is still `prometheus`. Micrometer 1.13 rewrote this
registry onto the new Prometheus Java client; the id is believed unchanged but has not been checked
on this stack.

**Deliverable**: a list of the metrics that arrive for free.

## Stage 1 — where metrics are exposed

`nginx/nginx.conf` currently routes `location /` to `app:8080` and `location /pub/` to
`publication:8080/`. As it stands, actuator would be reachable from outside under both, and the
public API has no authentication yet.

A separate `management.server.port` means the endpoints are not on 8080 at all, so no `location`
block can reach them however the file is edited later. No Spring Security, no path allowlist, nothing
to remember. The port is not published in compose `ports:` but stays reachable inside the network,
which is all stage 5 needs.

Endpoints: `health`, `prometheus`, `info`. Not `env` — it prints the whole configuration, including
the parts nobody meant to publish. `management.endpoint.health.show-details` stays `never`.

**Trap, and it costs half an hour if unknown.** Setting `management.server.port` creates a **separate
child context** for the management endpoints, which breaks the usual ways of reaching them from
`@SpringBootTest`. The standard answer is to put the endpoints back on the main port in
`application-test.yaml`.

## Stage 2 — health as the compose contract

Today only `db` has a `healthcheck`, via `pg_isready`. `app` and `publication` have none, so
`depends_on` can only wait for start, not for readiness.

`/actuator/health` supplies this ready-made, assembling indicators from the classpath: `DataSource`,
Flyway, Kafka. It matters most for `publication`, which currently looks alive even when it cannot
reach the broker.

Ordering with stage 1: the healthcheck must call the management port, not 8080.

## Stage 3a — consumer lag, for free

From the conditions report in an earlier build log:

```
KafkaMetricsAutoConfiguration:
  Did not match: @ConditionalOnClass did not find required class
  'io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics'
```

That class ships with `micrometer-core`, which is now on the classpath. No work beyond finding
`kafka_consumer_fetch_manager_records_lag_max` in the output and confirming the tags distinguish
topics.

## Stage 3b — a counter for messages sent to the DLT

The seam is the `DeadLetterPublishingRecoverer` bean in `DltProducerConfig`, which
`KafkaErrorHandlerConfig` passes to `DefaultErrorHandler`. `ConsumerRecordRecoverer` is a functional
interface, so a counting decorator is a few lines.

A counter resets on restart. That is not a defect to work around but a property to build the alert
on: the rule is `increase(...[1h]) > 0`, not `value > 0`. Pull-based collection over monotonic
counters is designed for exactly this.

## Stage 3c — two gauges over the outbox

`MeterRegistry` + `JdbcClient` in the monolith:

- stuck: `sent_at IS NULL AND dead_at IS NULL AND created < now() - interval '15 minutes'`
- dead: `dead_at IS NOT NULL`

Why 15 minutes and not an `attempts` threshold: `attempts` crosses any threshold within seconds of a
brief broker hiccup, so it cannot tell an outage from a stuck row. Time can. This is the same
reasoning that replaced attempt counts with exception-type classification in the relay.

Three things that catch people here:

1. **Micrometer holds a weak reference to a gauge's source object.** Register a gauge on a lambda
   capturing a local and the object is collected; the metric then reports `NaN`, silently. Using
   `this` on a Spring bean is safe, but the mechanism has to be known — `Gauge` is the only meter
   with this behaviour.
2. **The gauge callback runs on every scrape.** At the default 15s interval that is four `COUNT(*)`
   queries a minute. Negligible for one small table; the pattern for an expensive query is a cached
   value refreshed by `@Scheduled`.
3. **Name meters with dots, not underscores.** `protocols.outbox.pending`, not
   `protocols_outbox_pending`. Micrometer performs the Prometheus translation itself, adding `_total`
   to counters and a unit suffix. Writing Prometheus style by hand yields a double translation and a
   name matching no convention. `baseUnit("rows")` is not decoration — it reaches the final name.

## Stage 4 — tests

The patterns already exist in the repository and transfer almost verbatim.

- **Gauges**: as in `ProtocolOutboxCleanerIT` — Testcontainers, `TRUNCATE` in `@BeforeEach`, insert
  rows in several states, inject `MeterRegistry`, assert the value. The 15-minute boundary gets the
  same ±1 minute treatment as the retention boundary.
- **DLT counter**: in an existing ingest test, publish a message that cannot be retried —
  `IncorrectMessageKeyException` and `IncorrectProtocolIdException` are already registered as
  non-retryable — and assert the increment.

Coverage is not the constraint (87.9% and 91.0% against a 50% gate), but SQL inside a gauge with no
test is SQL nobody has checked.

## Stage 5 — Prometheus in compose

One container, one `prometheus.yml` with two scrape jobs, roughly fifteen lines. The Prometheus UI
graphs an expression, which is enough for three signals.

Without a scraper, lag cannot be read at all: a single value does not distinguish "behind and
catching up" from "behind and diverging". The same applies to the DLT counter, whose whole meaning is
its delta.

## Stage 6 — alert rules, which is the actual point

Without this stage the result is not observability but a page of numbers nobody opens. Three rules,
each with a `for` clause so a spike does not page:

- lag rising monotonically for N minutes;
- `increase()` of the DLT counter over a window > 0 — always an incident;
- `protocols.outbox.pending > 0` for more than a few minutes.

Honest boundary: actually **notifying** someone needs Alertmanager and a receiver, which is arguably
past the point for a pet project. The learning value is in writing the rules and watching them move
through `pending` to `firing` in the Prometheus UI. One local accident makes the last step cheaper
than usual though — the monolith already runs a Telegram bot, so an Alertmanager webhook into
Telegram is unusually little work here.

## Out of scope, deliberately

- **Distributed tracing.** `micrometer-observation` is already on the classpath transitively via
  `spring-web`, so it looks close at hand. It is not: tracing needs a backend (Tempo, Zipkin, Jaeger)
  and earns its keep across several hops. There is one hop here, and it is asynchronous — a Kafka
  topic, where a trace tells much less than it does across synchronous calls.
- **Log aggregation.** Worth having, unrelated to metrics, and a separate decision.
- **Hibernate statistics.** `generate_statistics` has to be enabled explicitly and costs something
  on every session. Not for a system with no known query problem.

## Commit boundaries

```
[Build]      actuator + micrometer in both modules          (done, uncommitted)
[Config]     management port, exposed endpoints, health
[Infra]      compose healthchecks for app and publication
[Monitoring] DLT counter + test
[Monitoring] outbox gauges + tests
[Infra]      prometheus container and alert rules
[Docs]       STATUS.md, README
```
