# Project Status

Current state of the monolith-to-services split and the ordered plan for the next work session.

Related: [Publication service design](publication-service-design.md),
[Multi-module conventions](multi-module-conventions.md).

**Last updated**: 2026-08-18

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

## Not started

`publication-service` and `publication-contract` contain a pom each and no code. No Kafka, no
second database.

## Plan for the next session

Frame the work as **one thin vertical slice**, not "finish the service": get a single protocol to
travel monolith → Kafka → consumer → one HTTP response, crudely, before deepening any layer.
Building the service in full first means designing its storage blind and bending the contract to
fit afterwards.

1. **`publication-contract` first.** A couple of records. It is the artifact both sides depend on,
   and writing it forces the decision about what crosses the boundary — which is the personal-data
   decision the whole split exists for. Shape it after
   `ProtocolRepository.ReportAssignmentProjection` plus protocol number and date, certification id,
   and the **computed** expiry date. No FK ids, nothing from `Tourist.contactinfo`.
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
