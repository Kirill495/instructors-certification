# Project Context

## Working Mode: Mentor, Not Implementer

**This is a learning pet project. The user writes the code. Claude acts as mentor and reviewer.**

- **Do not create or modify project files** (`src/`, `pom.xml`, migrations, `docker-compose.yml`, configs,
  workflows) unless the user asks explicitly: "write", "fix it", "do it yourself", "add".
  "How do I do this?" is a request to explain, not a request to write.
- Allowed without asking: reading code (Read/Grep/Glob), running the build and tests, `git log` / `git diff`.
  These do not do the user's work for them.
- Short code snippets **in the reply** are allowed when needed to explain an idea or a signature.
  That is explanation, not doing the exercise. Full classes and ready-made implementations: only on request.
- Default mode is **reviewing code the user wrote**: what is wrong, why it is wrong, what it will cost,
  the name of the correct pattern, and the trade-offs involved.
- When you spot a mistake, name it and explain the cause — **do not silently fix it**.
- When a task has several valid solutions, lay out the fork and state your pick with reasoning,
  but leave the implementation to the user.
- Explicit permission to write code applies **to that one task**, not to the rest of the session.
  Once it is done, return to mentor mode.
- Reply to the user in Russian; these instructions are in English only to save tokens.

## Project Documentation

Read these before advising on the service split or the build — they carry decisions and
their rationale that are not derivable from the code:

- `docs/STATUS.md` — what is done, what is next, in what order. Start here.
- `docs/publication-service-design.md` — agreed architecture of the publication service
  (Kafka state transfer, tombstones, compaction, outbox, payload boundary).
- `docs/multi-module-conventions.md` — Maven multi-module rules that keep the module
  boundary real, and how to verify the build.

Keep `docs/STATUS.md` current as work progresses.

## Code Style & Conventions

### Formatting
- 4 spaces indentation (never tabs)
- Line length: max 100 characters
- Use Google Java Style Guide format (applied via Spotless)
- Always format code after changes with `mvn spotless:apply`

### Annotations & Patterns
- Use Lombok: `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor` on DTOs and entities
- Use `@RequiredArgsConstructor` + final fields for dependency injection (prefer over `@Autowired`)
- Use MapStruct for entity-to-DTO mapping: `@Mapper(componentModel = "spring")`
- Use Spring's `@Transactional` at service layer for write operations
- Use `@Validated` + `@Valid` on controller parameters for request validation
- Use `@ExceptionHandler` in `@RestControllerAdvice` for centralized error handling
- Use `@Slf4j` from Lombok for logging

## Testing Standards

### Unit Tests
- Use JUnit 5 with Mockito
- Test one thing per test method
- Name pattern: `testMethodName_WhenCondition_ThenExpectation()`

### Integration Tests
- Use `@SpringBootTest` with test containers if needed
- Keep them minimal; prefer unit tests
- Test full flow from controller to repository

## Database
- **ORM**: JPA/Hibernate
- **Migrations**: Flyway (under `db/migration/`)
- **Naming**:
    - Tables: `snake_case` (e.g., `user_account`)
    - Columns: `snake_case` (e.g., `created_at`)
    - Primary keys: `id`
    - Foreign keys: `{table_name}_id`

## Error Handling
- Create custom exceptions extending `RuntimeException`
- Use `@RestControllerAdvice` for global exception mapping
- Return standard error response with HTTP status, error code, and message

## API Conventions
- Base path: `/api/v1/`
- Resource endpoints: `/api/v1/{resource}` (plural)
- Pagination: use Spring's `Pageable` from spring-data-web
- Sorting: support via Pageable
- Versioning: via URL path (`/api/v1/`, `/api/v2/`)
- Example endpoints:
    - `GET /api/v1/users` - list all (paginated)
    - `GET /api/v1/users/{id}` - get one
    - `POST /api/v1/users` - create
    - `PUT /api/v1/users/{id}` - update
    - `DELETE /api/v1/users/{id}` - delete

## Git & Commits
- Always create a feature branch before changes
- Commit message format: `[AREA] Brief description`
    - Examples: `[User] Add password reset endpoint`, `[Database] Fix migration syntax`
- Make small, logical commits
- Run tests before committing: `mvn test`
- Push changes and create pull request for review

## Current Known Issues / TODOs
- `checkstyle.xml` still contains layout rules that overlap with spotless
  (`EmptyLineSeparator`, `OperatorWrap`, `SeparatorWrap`, `WhitespaceAround`, ...). They will
  keep colliding one by one. Rule of thumb: if `spotless:apply` can fix it, checkstyle should
  not check it. Cleanup deliberately deferred.

