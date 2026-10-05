# Observability plan

The ordered plan for making both services observable. Item 1 of Next steps in
[STATUS.md](STATUS.md), split out because it is large enough to have its own decisions.

Related: [Publication service design](publication-service-design.md),
[Multi-module conventions](multi-module-conventions.md).

**Created**: 2026-09-27 · **Closed**: 2026-10-05, every stage done and every alert rule seen firing

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

**Verified 2026-09-27.** The endpoint id is still `prometheus` on this stack — Micrometer 1.13 rewrote
the registry onto the new Prometheus Java client, but the id did not move. Boot resolved
`micrometer-registry-prometheus` and `micrometer-core` to **1.16.3** from its BOM, and
`spring-boot-starter-actuator` to `4.0.3-SNAPSHOT`.

**Deliverable**: a list of the metrics that arrive for free.

**Done.** `include: health, info, prometheus` in both modules; the log confirms
`Exposing 3 endpoints beneath base path '/actuator'`.

One diagnostic worth keeping: probe with `-w "%{http_code}\n"` or `-i`, never bare `curl`. A `302` to
the login form has an empty body, so a plain `curl` prints nothing at all and looks identical to a
server that is not listening. Half an hour was spent on exactly that.

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

**Done 2026-09-27**, port `7001` in both services, published only in the dev overlay (`7001` for the
monolith, `7002` for publication, matching the existing `8081`/`8082` convention). `/pub/actuator/**`
through nginx went from `200` to `404`: the endpoint that was serving metrics to anyone who could
reach nginx is now unreachable from outside.

Two findings, both answering questions this section could not answer in advance:

1. **Spring Security does reach the management child context.** With a custom `SecurityFilterChain`
   present, `ManagementWebSecurityAutoConfiguration` backs off (`@ConditionalOnDefaultWebSecurity`),
   so the monolith's catch-all `webFilterChain` — which has no `securityMatcher` — covered
   `/actuator/**` on port 7001 as well, and every endpoint answered `302` to the login form. That
   would have meant no monolith metrics at all: Prometheus treats a redirect to an HTML page as a
   failed scrape and records `up = 0`.

   Fixed with an `@Order(0)` chain: `securityMatcher(EndpointRequest.toAnyEndpoint())`, then
   `requestMatchers(EndpointRequest.to("health", "info", "prometheus")).permitAll()` followed by
   `anyRequest().authenticated()`, plus `STATELESS` sessions and `csrf` disabled.

   Two reasons for that shape rather than the obvious one. `EndpointRequest` over a literal
   `"/actuator/**"` because the base path is configurable and a hardcoded string silently stops
   matching if `management.endpoints.web.base-path` is ever changed — leaving actuator behind the
   login form again with nothing to show for it. And naming the three endpoints, rather than
   `permitAll` on `toAnyEndpoint()`, so that **two independent locks** guard the rest: exposure
   decides what exists, security decides who may call it. Otherwise the `include` list is the only
   thing between the network and `heapdump`.

2. **`EndpointRequest` matchers are port-aware.** `AbstractRequestMatcher` consults
   `ManagementPortType`; when the management port differs, the matcher declines on the main-port
   context. So `permitAll` is not a hole on 8080, where nginx points, even if actuator were ever
   mapped back onto the application port.

`EndpointRequest.toAnyEndpoint()` does **not** cover the `/actuator` discovery page. The evidence was
the redirect itself: `Location: http://localhost:7001/login;jsessionid=…` — only `webFilterChain`
configures `formLogin`, and a chain with `SessionCreationPolicy.STATELESS` cannot mint a session, so
the request was never selected into the actuator chain. `EndpointRequest.toLinks()` is the matcher for
that page.

Opening it takes both places, which is the lesson: adding `toLinks()` to `requestMatchers` alone is
dead code, because `securityMatcher` decides what enters the chain at all. The working form wraps both
matchers in an `OrRequestMatcher` in `securityMatcher`, then permits links inside. The two levels are
easy to conflate — `securityMatcher` is navigation, `requestMatchers` is authorization.

Worth having rather than essential: the page lists URLs and nothing else, but it answers "what is
actually exposed here" in one request instead of from memory.

## Stage 2 — health as the compose contract

Today only `db` has a `healthcheck`, via `pg_isready`. `app` and `publication` have none, so
`depends_on` can only wait for start, not for readiness.

`/actuator/health` supplies this ready-made — nothing to write. But **check which indicators actually
exist before relying on one.** Verified against the Boot 4 jars, which split auto-configuration into
per-technology modules:

| Indicator | Module | Present |
|---|---|---|
| `db` — `DataSourceHealthIndicator` | `spring-boot-jdbc` | yes |
| `diskSpace`, `ping`, `ssl` | `spring-boot-health` | yes |
| `livenessState`, `readinessState` | `spring-boot-health` | yes |
| **Kafka** | `spring-boot-kafka` | **no** |
| **Flyway** | `spring-boot-flyway` | **no** |

An earlier draft of this document claimed health would assemble Kafka and Flyway indicators. It does
not: a search across every `spring-boot-*` and `spring-kafka` jar finds neither. So `/actuator/health`
reports `UP` on `publication` with a dead broker, and the signal that matters most for that service is
not supplied.

That is a decision, not just a gap. A custom Kafka indicator is little code
(`AdminClient.describeCluster()` with an explicit timeout), but consider what `DOWN` would mean:
`depends_on: service_healthy` would stop admitting nginx, while **the read API keeps working without
Kafka** — `ProtocolRegistry` serves from the service's own database. A working service would be taken
out of rotation because something it needs only for ingest is unavailable. So broker unavailability
belongs in metrics and alerts (stages 3a and 6), not in the default health group. If an indicator is
added anyway, put it in a separate health group, with a timeout shorter than the healthcheck's — an
indicator that calls an external system without one turns the health probe into a hang.

Flyway needs nothing: a failed migration stops startup outright, and `fail-on-missing-locations: true`
is already set. That failure shows up as a missing container, not as a health status.

**Use the full `/actuator/health`, not the readiness group.** By default `readiness` contains only
`readinessState`, an in-process flag from `ApplicationAvailability` — `db` is not in it, so
`/actuator/health/readiness` answers `UP` against a dead database. The ungrouped endpoint includes
`db` and `diskSpace`.

Ordering with stage 1: the healthcheck must call the management port, not 8080. Doing it the other way
round produces a **false-positive** check — `curl -f` fails only on codes ≥ 400, so the `302` to the
login form counts as success, and Docker would report the monolith healthy whatever state it is in.

`curl` and `wget` are both present in `eclipse-temurin:21-jre` (checked). A move to an alpine or
distroless base would break the healthcheck command itself, with the symptom of a permanently
`unhealthy` but perfectly working container.

Set `start_period` — without it Docker counts failures from the first second, and Boot with Hibernate
and Flyway takes long enough to reach `unhealthy` before its first successful reply.

**Done 2026-09-27.** Both services report `(healthy)`; `nginx` now waits for readiness rather than for
container creation, so the `502`s in the first seconds after `up` are gone. The effective check, read
back from the running container:

```
CMD-SHELL curl -f http://localhost:7001/actuator/health
Interval 30s   Timeout 3s   Retries 3   StartPeriod 40s   StartInterval 2s
```

### `start_period` and `start_interval` are different axes

They are complementary, not alternatives, and the second one is easy to miss.

- **`start_period`** is an amnesty window, not a deadline: failures inside it do not count towards
  `retries` and the container reports `starting`. A success inside it ends the window immediately. Once
  it expires, failures start counting, so the real limit of patience is
  `start_period + retries × interval` — here 40 + 3×30 = **130 s**.
- **`start_interval`** is how often the check runs *while inside* that window; outside it, `interval`
  applies. It needs Docker 25.0 / API 1.44 (this machine is on 25.0.3, API 1.44 — available, only just).

Without it the two frequencies are one, forcing a bad trade: `interval: 30s` learns of readiness up to
half a minute late, while `interval: 5s` polls a healthy service forever — 17 280 checks a day, each one
an HTTP request that runs the `db` indicator and therefore a validation query. `start_interval` splits
them: poll fast while starting, slowly once up.

An `unhealthy` container is **not** restarted by Docker on its own. The consequence is quieter than a
restart loop: `depends_on: condition: service_healthy` simply never unblocks and `nginx` never starts.

### Sizing it from measurement, not from guesswork

`Started … in N seconds` turned out to be **absent from the logs of both services** — and so are the
other two `StartupInfoLogger` lines, `Starting … using Java …` and `No active profile set`. All three
sit behind one `logStartupInfo` flag, which looks switched off, but nothing in the project sets
`spring.main.log-startup-info`, there is no logback config, and the banner does print. Boot 4 still has
`logStarting`, `logStarted` and `process running for` in `StartupInfoLogger`, so the messages were not
renamed. Cause unidentified; cosmetic, and worth one experiment if it ever matters — set the property
explicitly to `true` and see whether all three come back.

Measure the interval that actually matters instead: container start to the management port accepting
connections.

| Service | Container start | Port 7001 up | Delay |
|---|---|---|---|
| `app` | 10:20:16.525 | 10:20:27.475 | **10.95 s** |
| `publication` | 10:20:16.678 | 10:20:23.922 | **7.24 s** |

So `publication` is faster, but by a factor of 1.5, not an order of magnitude — an initial guess of
`start_period: 5s` for it was under the real figure. Against 40 s the margins are 3.6× and 5.5×, and
`start_interval: 2s` means health is established around second 9 and second 13 instead of at second 30.

The precise record is Docker's own, though it keeps only the **last five** checks, so it has to be read
soon after `up`:

```
docker inspect -f '{{range .State.Health.Log}}{{.Start}} {{.ExitCode}}{{"\n"}}{{end}}' <id>
```

### Two traps that cost time here

**`CMD SHELL` instead of `CMD-SHELL`.** A missing hyphen, and Compose refuses to parse the file at all:
`healthcheck.test must start either by "CMD", "CMD-SHELL" or "NONE"`. Not a warning about one service —
`ps`, `logs` and `up` all fail identically, which makes it look like something far worse than a typo.
`docker compose config` validates the merged files in a second and would have caught it without
touching a container. Worth running whenever the compose files change, dev overlay included.

**Logs redirected from PowerShell are UTF-16.** `>` and `*>` write UTF-16 LE with a BOM (`ff fe`, then
`a \0 p \0 p \0`), and every line-based search over such a file finds nothing, because the bytes do not
match an ASCII pattern. Use `| Out-File -Encoding utf8`. And grep is not required on Windows:
`Select-String` in PowerShell, `findstr` in cmd.

## Stage 3a — consumer lag, for free

From the conditions report in an earlier build log:

```
KafkaMetricsAutoConfiguration:
  Did not match: @ConditionalOnClass did not find required class
  'io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics'
```

That class ships with `micrometer-core`, which is now on the classpath.

**Done 2026-09-27**, and it arrived with four corrections to the assumption above. The metrics live on
`publication` only — the consumer is there, not in the monolith — so this is port **7002** on the host.
Of roughly 200 series, **143** are `kafka_consumer_*`.

### Use `records_lag`, not `records_lag_max`

The metric this document originally named is unusable here:

```
kafka_consumer_fetch_manager_records_lag{…,partition="0",topic="protocols.snapshots"}      0.0
kafka_consumer_fetch_manager_records_lag_avg{…,partition="0",topic="protocols.snapshots"}  NaN
kafka_consumer_fetch_manager_records_lag_max{…,partition="0",topic="protocols.snapshots"}  NaN
```

`records-lag` is the **latest** per-partition lag, a plain value. `records-lag-avg` and `records-lag-max`
are computed over a **sampled window**, and with no fetch carrying records inside that window they
report `NaN`. In this system that is the normal state: snapshots are published only when a protocol
changes, so the windowed variants are `NaN` almost always.

This is a trap with no symptom. PromQL comparisons against `NaN` are always false, so
`records_lag_max > N` **never fires** — and an alert that cannot fire looks exactly like an alert that
never needed to. `NaN` is not zero, and here it does not even mean "no lag", it means "no sample".

### Lag alone cannot see a stuck consumer — two companions that can

Recorded in STATUS.md as "a stuck partition is silent". Lag does not close it: if the listener thread
hangs, `records_lag` keeps reporting its last value while the real backlog grows. Two metrics in the
same dump do close it:

| Metric | Value observed | What it detects |
|---|---|---|
| `kafka_consumer_coordinator_assigned_partitions` | `1.0` | drops to `0` when the consumer leaves the group or rebalances forever |
| `kafka_consumer_last_poll_seconds_ago` | `0.0` | grows without bound when the poll loop is stuck |

The second is the direct detector for the silent-stuck-partition case, and it costs nothing. Alert on
these two rather than on lag alone.

Also present and worth knowing: `kafka_consumer_time_between_poll_avg` reads `5000.7` ms, which is
Spring Kafka's `ContainerProperties` default poll timeout of 5 s showing through on an idle consumer —
not a problem, but it explains a number that otherwise looks alarming.

### Every topic-labelled series is duplicated

```
topic="protocols.snapshots"   ← the real name
topic="protocols_snapshots"   ← periods replaced, deprecated per the metric's own HELP text
```

The Kafka client emits both for any topic name containing a period. Match on the real name in every
rule: an aggregation such as `sum()` over the family double-counts, and the deprecated variant will
eventually disappear and take a silently-matching rule with it.

### Bonus: the startup time that was missing from the logs

`application_started_time_seconds` **6.879** and `application_ready_time_seconds` **6.887**, tagged
`main_application_class="org.tourism.publication.PublicationServiceApplication"`. That is the figure
hunted for in stage 2 through the absent `Started …` log line — available as a metric all along, and it
agrees with the 7.24 s measured from container start (the difference is JVM launch before Spring
begins). Note also that `main_application_class` is populated, so whatever suppresses those three log
lines, it is not a null `mainApplicationClass`.

### One metric that will matter in stage 3b

`spring_kafka_listener_seconds{name="…KafkaListenerEndpointContainer#0-0",result,exception}` times the
listener and tags the outcome. It is **not** a substitute for the DLT counter — a retryable failure
increments `result="failure"` on every attempt, and the backoff is unlimited, so the count says
"something is failing", not "a message was given up on". But as an early warning it is free and it moves
long before anything reaches the DLT.

## Stage 3b — a counter for messages sent to the DLT

The seam is the `DeadLetterPublishingRecoverer` bean in `DltProducerConfig`, which
`KafkaErrorHandlerConfig` passes to `DefaultErrorHandler`. `ConsumerRecordRecoverer` is a functional
interface, so a counting decorator is a few lines.

A counter resets on restart. That is not a defect to work around but a property to build the alert
on: the rule is `increase(...[1h]) > 0`, not `value > 0`. Pull-based collection over monotonic
counters is designed for exactly this.

**Done 2026-09-28**, but not at that seam, and only after a prerequisite bug fix.

### The seam is `IngestRetryListener`, not a decorator round the recoverer

`RetryListener.recovered(record, ex)` fires **after** a successful hand-off to the DLT, so it counts
what happened rather than what was attempted; a decorator round the recoverer counts attempts.
`IngestRetryListener` already existed and already logged this exact event, so the metric went next to
the log rather than into a new class. It also came with `recoveryFailed(...)` for free — the more
urgent incident of the two, since a record that never reached the DLT is parked nowhere.

One name, `publication.ingest.dlt.records`, with tags `result` (`sent`/`failed`) and `exception` (the
simple class name). Tag keys match `spring_kafka_listener_seconds{exception,result}`, so the two
metrics can be compared in one query. `record.key()` is deliberately **not** a tag: it is a protocol
id, which would mint a time series per protocol.

Because `exception` is only known at call time, the counter is registered **lazily, on first
increment**. It therefore does not exist at all until the first record is dead-lettered. An alert on
`increase(...)` copes with an absent series (no data, no alert); a dashboard panel shows "No data"
rather than a truthful zero. That is the price of a dynamic tag, and it is predictable.

### First the queue had to stop swallowing the main failure mode

`UnsupportedSnapshotVersionException` was thrown by the listener but **not** registered in
`addNotRetryableExceptions`. `DefaultErrorHandler` classifies a plain `RuntimeException` as retryable,
and with `FixedBackOff(DEFAULT_INTERVAL, UNLIMITED_ATTEMPTS)` that means redelivery forever and a
partition blocked for good. A version mismatch is the most likely failure during a contract change —
precisely what the version field exists for — and it would never have reached the DLT, so the new
counter would have read `0` while the consumer sat wedged. Fixed before the counter was written;
writing the metric first would have produced a metric that is silent on its main case.

### Three Micrometer lessons paid for in bugs

1. **`registry.counter(name, tags…)` takes alternating key/value varargs.** One string
   `"result:sent"` is an odd argument count and throws from `Tags.of`.
2. **It returns the `Counter`; nothing happens without `.increment()`.** The meter registers at `0`
   and stays there — a metric that is present, looks healthy and measures nothing.
3. **Resolving the counter per call is correct here, not sloppy.** The `exception` tag is only known
   at call time, so the usual "register meters once in the constructor" advice does not apply;
   `counter(...)` is a find-or-create over a concurrent map, which is what Micrometer expects.

`Counter` has no weak-reference hazard — that one belongs to `Gauge` alone.

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

**Done 2026-09-28**, and the shape changed on the way: the 15-minute threshold left the metric.

Three gauges under two names — `protocols.outbox.rows{state="pending"|"dead"}` and
`protocols.outbox.oldest.pending.age` with `baseUnit("seconds")`. Counting rows *older than fifteen
minutes* would compile the alerting policy into Java: changing it to five would mean a rebuild and a
deploy, and the metric could never answer "how old is the backlog", only "how many crossed a line
chosen at compile time". Exporting the age leaves the threshold in the Prometheus rule, where a change
is a config reload. Age is also the stronger signal — three rows fifteen minutes old and three rows six
hours old are the same count and very different incidents.

The component is `ProtocolOutboxMetrics`, deliberately **not** part of `ProtocolOutboxRelay`. The relay
carries `@ConditionalOnProperty("protocols.outbox.relay.enabled")`, so a gauge living inside it would
vanish exactly when the relay is switched off — the moment the queue starts growing and the metric
matters most. An observation must not share a kill switch with the thing it observes.

One `@Scheduled` refresh every five minutes into an immutable record, rather than a query per scrape.
The trade is predictable load instead of load proportional to the number of scrapers, at the cost of up
to five minutes of staleness — which stacks with `for:` in the alert rule, so a fifteen-minute
threshold detects at roughly twenty-five minutes. Three separate scheduled tasks were collapsed into
one: the default `spring.task.scheduling.pool.size` is **1**, shared with the relay's one-second tick.

Four bugs worth remembering, all silent:

1. **`metricsData::pendingRows` is a bound method reference.** It captures the record instance that
   exists when the reference is created, and `refreshMetrics()` reassigns the field — so every gauge
   reported `0` forever. `() -> field.method()` reads the field per call; `field::method` does not.
2. **`DefaultGauge` holds `WeakReference<T>`, and with `Gauge.builder(String, Supplier<T>)` the
   supplier *is* `T`.** An inline lambda or method reference has no other owner and can be collected,
   after which the gauge reports `NaN`. `Gauge.builder(name, this, m -> m.field.value())` fixes both
   bugs at once: `this` is a Spring bean, held strongly by the context.
3. **`FILTER` is part of the aggregate call syntax.** `EXTRACT(EPOCH FROM now() - min(x)) FILTER (…)`
   is a syntax error; the clause belongs on `min(x)`, inside `EXTRACT`. All three aggregates must also
   use the *same* predicate, or one row of output contradicts itself.
4. **`timestamptz - timestamptz` is an `interval`, and `min()` over no rows is `NULL`.** Hence
   `EXTRACT(EPOCH FROM …)` and `COALESCE(…, 0)`.

The field is `volatile`: the scheduler thread writes it and the scrape thread reads it. An immutable
record is half of safe publication; `volatile` is the other half.

## Stage 4 — tests

**Done 2026-09-29.** `ProtocolOutboxMetricsIT` (four cases), `IngestRetryListenerTest` (three), and two
tests in `ProtocolSnapshotListenerIT`. The plan's "the 15-minute boundary gets the same ±1 minute
treatment as the retention boundary" is **obsolete, not skipped** — the threshold moved into the alert
rule, so there is no boundary in the code to test.

### A test must be built so that the known-wrong version gives a different answer

The first draft of the gauge IT put every row at the same `created_at`, which made `min(created_at)`
identical with or without the `dead_at`/`sent_at` filters: it passed against the broken SQL. Staggering
the rows — dead an hour old, sent ninety seconds old, live unsent ten seconds old — makes each missing
predicate produce its own recognisable number: 3600, 90, 10.

Confirmed by mutation: removing `dead_at IS NULL` from the age filter fails two tests with
`получено 3600`; restoring it turns them green. A test nobody has watched fail is a test of unknown
value.

Counts per state are deliberately different (2 pending / 1 dead). With equal counts, swapping the
`state` tags between the two `Gauge.builder` calls — the likeliest copy-paste error there — stays
invisible.

### `(int) Double.NaN` is `0`

Truncating a gauge value to `int` erases the one signal that separates "no data" from "zero", so a
collected gauge source reads as an honest zero. Check `Double.isNaN` **before** the cast.
`assertNotNull` on a `double` is a no-op: the primitive boxes to a never-null `Double`.

### Prefer `@Autowired` over hand-building the bean

An earlier draft constructed `ProtocolOutboxMetrics` itself with a `SimpleMeterRegistry` and called
`register()` by hand. That leaves `@Component` and `@PostConstruct` unverified — delete either and every
test stays green while production exports nothing. Injecting the bean and the context's registry costs
nothing and closes the gap. There is no scheduler race to dodge here, unlike `ProtocolOutboxRelayIT`: a
`fixedRate` of five minutes fires once at startup and not again inside the test's lifetime.

### Kafka ITs share mutable state that nothing resets

Three mechanisms keep the outbox ITs deterministic: a fresh container, `TRUNCATE` in `@BeforeEach`, and
`protocols.outbox.relay.enabled: false` in the test profile. In `ProtocolSnapshotListenerIT` only the
second applies, and only to the database. The DLT topic and the `MeterRegistry` live for the whole class
and cannot be cleared — a consumer with a fresh group and `auto.offset.reset=earliest` sees everything
ever written to the topic.

Both symptoms of that came from one root: two DLT tests using the message key `101`, so `findFirst()`
over the earliest-reading consumer returned **the other test's record** — 17 bytes of `это не json`
where 332 were expected — or none at all, depending on order. Isolation here is a matter of
construction, not cleanup: **a unique key per test**, and **relative** assertions on counters
(`after == before + 1`, never `== 1`, since the registry is shared and both tests increment).

The baseline for such a delta cannot be read inside `untilAsserted`. The lambda runs repeatedly, so the
expected value moves with the actual one and the condition becomes unsatisfiable by construction — it
never goes green rather than going flaky. Everything inside `untilAsserted` must be re-evaluable; the
snapshot of "before" is the one thing that must live outside it.

### The two DLT paths differ in three ways, so their assertions do not transfer

| | Deserialization failure | Business-rule failure |
|---|---|---|
| DLT value | the original raw bytes | the parsed `ProtocolSnapshot`, **re-serialised** |
| `DLT_EXCEPTION_FQCN` | `DeserializationException` | `ListenerExecutionFailedException` |
| Business exception | — | `DLT_EXCEPTION_CAUSE_FQCN` (the root cause) |

`DltProducerConfig`'s own javadoc states the first row; the second and third follow from
`DeadLetterPublishingRecoverer.addExceptionInfoHeaders`, which writes `exception.getClass()` to one
header and `ErrorHandlingUtils.findRootCause(exception).getClass()` to the other. Copying a byte-array
comparison from the deserialization test into the version test is therefore wrong in principle, even
though it happened to be off by exactly the text block's trailing newline.

Coverage is not the constraint (87.9% and 91.0% against a 50% gate), but SQL inside a gauge with no test
is SQL nobody has checked.

## Stage 5 — Prometheus in compose

One container, one `prometheus.yml` with two scrape jobs, roughly fifteen lines. The Prometheus UI
graphs an expression, which is enough for three signals.

Without a scraper, lag cannot be read at all: a single value does not distinguish "behind and
catching up" from "behind and diverging". The same applies to the DLT counter, whose whole meaning is
its delta.

**Done 2026-09-30.** `prom/prometheus:v2.54.1`, config bind-mounted read-only, TSDB on a named volume
at `/prometheus`, UI published only in the dev overlay as `7070:9090`, no `depends_on`. Verified
end-to-end: `/api/v1/status/config` reports our two jobs, both targets `health: up` with an empty
`lastError`, and `up`, `protocols_outbox_rows{state="pending"|"dead"}` and
`protocols_outbox_oldest_pending_age_seconds` all return series. 354 metric names in total, three of
them written by hand.

`job` and `instance` are attached by **Prometheus**, not by the application — `job` from `job_name`,
`instance` from the target address. That is why they are absent from `/actuator/prometheus` yet
available to filter on in every rule.

### `command:` replaces the image's `CMD`; it does not extend it

`prom/prometheus` ships `CMD` with four flags, including `--config.file=/etc/prometheus/prometheus.yml`
and `--storage.tsdb.path=/prometheus`. Setting `command:` for one flag of our own dropped all four.
`ENTRYPOINT` is *not* replaced, so the binary still launched and complained about a flag — a symptom
that says nothing about the lost paths. Losing `--config.file` is the expensive one: the built-in
default is `prometheus.yml` relative to the image's WORKDIR of `/prometheus`, which does not exist, and
the error then sends you to check a bind mount that was never broken. `--storage.tsdb.path` defaults to
`data/`, which lands inside the mounted volume by luck.

Read the image's `CMD` before overriding it — `docker image inspect <image> -f '{{json .Config.Cmd}}'`
— and re-list whatever of it you still need. The two console-template flags are legacy and safe to
drop. This is the same shape as `management.endpoints.web.exposure.include`: the list replaces, it does
not add.

### `docker compose config` validates the model, not the commands

It passed cleanly over two different mistyped flags (`--web.enable--lifecycle`, then
`-storage.tsdb.path`). It checks schema, volume and network references and variable substitution —
never flag names, never whether the image exists. Prometheus reports a single-dash long flag as
`unknown short flag '-s'`, naming a letter you never typed, so the message does not point at the typo.

Run the first start **without** `-d`. `up -d` prints `Created / Starting / Started` for a container that
lived under a second, and `docker compose ps` hides exited containers unless given `-a`.

### Stale-DNS is an nginx problem, not a Prometheus one

nginx resolves a static `proxy_pass` host once at configuration parse time and caches the address for
the life of the process. Prometheus keeps the hostname in `static_configs` and resolves at dial time, so
recreating `app` or `publication` costs one failed scrape and then recovers by itself. Reloading
Prometheus after a rebuild is unnecessary; reloading nginx is mandatory.

Its own config, being bind-mounted, reloads with `docker compose kill -s HUP prometheus` — the same
signal `nginx -s reload` sends. `POST /-/reload` needs `--web.enable-lifecycle`, which also exposes
`POST /-/quit`; on a port published to `0.0.0.0` that is a shutdown switch for anyone on the network.

## Recreating a container: nginx caches upstream addresses

Hit twice while doing stage 1, and it will be hit again the moment the Prometheus container joins the
compose file. Worth knowing before it costs an evening.

`nginx.conf` names its upstreams literally:

```
proxy_pass http://app:8080;
```

With no variable and no `resolver`, nginx resolves that name **once, when it loads the configuration**,
and caches the address for the life of the process. Rebuild `app` and `publication` — with
`docker compose up -d --build app publication`, which is the right command, since `down` would restart
Kafka and force a consumer-group rebalance for nothing — and the new containers come up with new
addresses while nginx keeps the old ones.

The failure is nastier than the `502` one would expect. On 2026-09-27 the two containers **exchanged**
addresses on recreation, so nginx was still pointing each route at the address the *other* service now
held:

```
:80/           404   {"status":404,"error":"Not Found","path":"/"}   ← publication answering
:80/login      404                                                   ← publication has no /login
:80/pub/       302 → http://localhost/login                          ← the monolith
:80/pub/login  200                                                   ← the monolith's login page
```

Docker's DNS was correct the whole time (`getent hosts app publication` inside the nginx container
resolved both correctly); only nginx's cache was stale. The routes were silently swapped, every
response was a plausible HTTP code, and `404` on `/` reads exactly like a routing mistake in a file
that had not been touched.

The fix is one command:

```
docker compose exec nginx nginx -s reload
```

Make it a habit after recreating anything nginx proxies to. In PowerShell that is two commands on two
lines — `&&` is not a pipeline operator there.

The permanent fix is a variable in `proxy_pass` plus `resolver 127.0.0.11 valid=10s;`, the address of
Docker's embedded DNS, which re-resolves per request with a short cache. Deliberately not done: with a
variable nginx stops normalising the URI, so `location /pub/` would have to spell the path out, and
that is a change to routing in exchange for remembering one command.

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

**Done 2026-10-02.** Eight rules in `prometheus/rules/alerts.yml`, three groups
(`instructors_availability`, `monolith_outbox`, `publication_ingest`), all eight loading with
`health: ok`. `ServiceDown` has been verified end to end — it went `pending` → `firing` when
`publication` was stopped. `DeadOutboxRows` and `OutboxNotDraining` followed by provocation on
2026-10-05 — see the last subsection of this stage.

### Severity has to answer one question, or it answers none

The first draft had six `warning` and one `critical`, and two rules at `info` that were strictly worse
than a `warning` one: `ConsumerPollStalled` and `ConsumerHasNoPartitions` mean nothing is being
consumed at all, so lag grows without bound, while `ConsumerLag` is only the symptom and may clear
itself. The two are also stages of one failure — a poll loop stalled past `max.poll.interval.ms` gets
the consumer evicted from the group, which is the second alert.

The axis that fixed it: **is the pipeline down, or are individual records affected?**

| `critical` — pipeline down | `warning` — individual records |
|---|---|
| `ServiceDown` | `DeadOutboxRows` |
| `ConsumerPollStalled` | `OutboxNotDraining` |
| `ConsumerHasNoPartitions` | `MessagesGoTo_DLT` |
| `MessagesFailedGoTo_DLT` | `ConsumerLag` |

`MessagesFailedGoTo_DLT` outranks `MessagesGoTo_DLT` because a record that never reached the DLT is
stored nowhere and will be redelivered — which risks blocking the partition. One that did reach it is
parked and waiting.

### `promtool check rules` parses templates but does not execute them

The sharpest finding of the stage. `{{ value }}` — no `$` — passes validation, loads, and breaks **when
the alert fires**, which is the worst possible moment. Proven with a two-rule probe file:

```
summary: "{{ value }}"        → promtool says nothing
summary: "{{ nosuchfunc }}"   → FAILED: function "nosuchfunc" not defined
```

`value` *is* a Prometheus template function (it takes a sample and returns its number), so the parser
is satisfied; argument count is checked at execution. An unknown name fails at parse time, a known name
misused does not.

So the tool boundary here mirrors `docker compose config`, which validates the compose model but not
flag names: **every check has an edge, and knowing it is cheaper than discovering it in an incident.**
An earlier template bug in the same file — `{{ $protocols_outbox_rows{state=\"dead\"} }}` — *was* caught,
because a `{` inside an action is a parse error. It also took down the whole process: one bad template
fails the entire config load, not just its rule.

Annotations have no access to metrics by name. Only `$value`, `$labels` and `$externalLabels` exist.

### Three habits the rules file taught

**Reload is a third step, easy to forget.** Editing the file and running `promtool` says nothing about
what the running process holds: the mount is live, the in-memory copy is not. Four rules sat invisible
until `POST /-/reload`. The cycle is edit → `promtool check rules` → reload.

**`health: unknown` right after a reload is not an error**, just "not evaluated yet"; with
`evaluation_interval: 15s` it clears in seconds.

**`ALERTS` is how you answer "did it fire an hour ago".** A resolved alert disappears from
`/api/v1/alerts` completely, but Prometheus records a synthetic `ALERTS{alertname, alertstate, …}`
series while a rule is pending or firing, so `max_over_time(ALERTS[1h])` shows the history. That is how
`ServiceDown` was confirmed after the service was already back up.

### Do not put a threshold in the annotation text

`"… больше 60 секунд. {{ $value }}"` hardcodes the threshold in prose, so changing `expr` leaves the
text lying — the same defect as compiling a policy into a metric, which is what moved the 15 minutes out
of Java in the first place. The number is also redundant: `$value` already prints the measurement.

Other small rules learned the same way: `$value` carries nothing when the expression is an equality
against a constant (`== 0` can only ever print `0`; use `$labels` instead); `humanizeDuration` turns
`312` into `5m 12s` for a seconds-valued metric and `humanize` turns `14000` into `14k` for a count;
and a topic pinned in `expr` should be printed from `{{ $labels.topic }}` rather than retyped.

### The last two rules, proven by provocation (2026-10-05)

Both fired against the running stack:

- **`DeadOutboxRows`** — provoked with an outbox row carrying `dead_at`.
- **`OutboxNotDraining`** — provoked by stopping Kafka and publishing a protocol, with the threshold
  lowered from `900` to `30` for the run. At 900 s plus the five-minute gauge refresh the honest wait is
  about twenty-five minutes; lowering the number tests the same rule.

**A provocation leaves residue, and the residue is itself an alert.** Both experiments change state that
outlives them, and each has a cost if left behind:

| Left behind | Cost |
|---|---|
| The threshold at `30` in `alerts.yml` | Fires on every broker hiccup longer than half a minute — the noise the time-based threshold exists to avoid. One stray `git add -A` commits it. |
| The test row with `dead_at` | Nothing removes dead rows — retention skips them by design — so `DeadOutboxRows` keeps firing, and the first real dead row arrives into an alert that is already on. |

Cleanup is part of the experiment, not after it: `git restore` the rule file **and** reload Prometheus,
since the in-memory rules survive the edit on disk (the "reload is a third step" habit above, in
reverse); delete the row; then confirm in the UI that both rules are back to `inactive`.

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
[Config]     actuator + micrometer in both modules          committed  9ad8c81
[Config]     management port, exposed endpoints, security   committed  4ceb95c
[Infra]      compose healthchecks for app and publication   done
[Monitoring] DLT counter + non-retryable fix + tests        done
[Monitoring] outbox gauges + tests                          done
[Infra]      prometheus container, create_host_path debt     done
[Infra]      alert rules                                     done
[Docs]       STATUS.md, README, this plan                    done
```

## Where this stands

| Stage | State |
|---|---|
| 0 — inventory of free metrics | done 2026-09-27 |
| 1 — management port, exposure, security | done 2026-09-27 |
| 2 — compose healthchecks | done 2026-09-27 |
| 3a — consumer lag | done 2026-09-27; no code, but four corrections to the plan |
| 3b — DLT counter | done 2026-09-28, after fixing an infinite retry that hid its main case |
| 3c — outbox gauges | done 2026-09-28; the 15-minute threshold moved into the alert rule |
| 4 — tests for 3b and 3c | done 2026-09-29; gauge tests verified by mutation |
| 5 — Prometheus container | done 2026-09-30; both targets up, custom metrics stored |
| 6 — alert rules | done 2026-10-02; eight rules, all seen firing — the last two provoked 2026-10-05 |

Nothing remains. Everything listed under "Out of scope, deliberately" stays out of scope.

Nothing in stages 0–2 required application code beyond one Spring Security filter chain: the rest was
configuration. Stage 3a is the last free one — from 3b onwards it is code and tests.
