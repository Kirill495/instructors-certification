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
