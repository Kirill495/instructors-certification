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

## Environment & Build
- **Java Version**: 21
- **Spring Boot**: 4.0.3-SNAPSHOT
- **Build Tool**: Maven
- **Build Commands**:
    - `mvn clean install` - build with tests
    - `mvn clean install -DskipTests` - build without tests
    - `mvn test` - run all tests
    - `mvn spring-boot:run` - run application locally
    - `mvn spotless:apply` - format code with Google Java style

## Project Structure
```
src/main/java/
├── controller/       - REST endpoints, @RestController, request validation
├── service/          - business logic, @Service, transactions
├── repository/       - data access, @Repository, JPA interfaces
├── model/            - entity classes, @Entity, DTOs
├── config/           - Spring configuration, @Configuration
├── exception/        - custom exceptions, exception handlers
├── util/             - utilities, constants
└── mapper/           - entity <-> DTO mapping (MapStruct)

src/test/java/
├── controller/       - integration tests with @SpringBootTest
├── service/          - unit tests with @ExtendWith(MockitoExtension.class)
└── repository/       - data layer tests
```

## Code Style & Conventions

### Naming
- Classes: `PascalCase` (e.g., `UserService`, `CreateUserRequest`)
- Methods: `camelCase` (e.g., `getUserById()`, `createUser()`)
- Constants: `UPPER_SNAKE_CASE` (e.g., `DEFAULT_PAGE_SIZE = 20`)
- Private fields: `camelCase` with no prefix (e.g., `private String userName;`)

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

### Example Service Class
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Transactional(readOnly = true)
  public UserDto getUserById(Long id) {
    log.info("Fetching user with id: {}", id);
    return userRepository.findById(id)
        .map(userMapper::toDto)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }

  @Transactional
  public UserDto createUser(CreateUserRequest request) {
    // validation happens at controller level via @Valid
    var user = userMapper.toEntity(request);
    var saved = userRepository.save(user);
    log.info("User created with id: {}", saved.getId());
    return userMapper.toDto(saved);
  }
}
```

## Testing Standards

### Unit Tests
- Use JUnit 5 with Mockito
- Test one thing per test method
- Name pattern: `testMethodName_WhenCondition_ThenExpectation()`
- Example:
  ```java
  @ExtendWith(MockitoExtension.class)
  class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserService userService;

    @Test
    void testGetUserById_WhenUserExists_ThenReturnUserDto() {
      // Arrange
      var user = new User(1L, "John");
      when(userRepository.findById(1L)).thenReturn(Optional.of(user));
      
      // Act
      var result = userService.getUserById(1L);
      
      // Assert
      assertThat(result).isNotNull();
      assertThat(result.getName()).isEqualTo("John");
    }
  }
  ```

### Integration Tests
- Use `@SpringBootTest` with test containers if needed
- Keep them minimal; prefer unit tests
- Test full flow from controller to repository

## Key Dependencies
- `spring-boot-starter-web` - REST API
- `spring-boot-starter-data-jpa` - database access
- `lombok` - reduce boilerplate
- `mapstruct` - entity mapping
- `jakarta.validation:jakarta.validation-api` - input validation
- `org.springframework.boot:spring-boot-starter-validation` - validator
- `h2` - in-memory DB for tests
- `org.junit.jupiter:junit-jupiter` - testing
- `org.mockito:mockito-core` - mocking
- `io.rest-assured:rest-assured` - API testing (optional)

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
- Example custom exception:
  ```java
  public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
      super(message);
    }
  }
  ```

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

## Code Review Checklist
When reviewing or preparing code for review, check:
- [ ] Tests are written and passing (`mvn test`)
- [ ] Code is formatted (`mvn spotless:apply`)
- [ ] No TODOs left without issue numbers
- [ ] Exception handling is proper
- [ ] `@Transactional` is used correctly (read-only where applicable)
- [ ] No N+1 queries (check JPA fetch strategies)
- [ ] Logging is appropriate (not too verbose, not missing)
- [ ] API contracts are backward compatible or versioned
- [ ] Security: no hardcoded secrets, proper validation
- [ ] Documentation: javadoc on public methods

## Useful Commands Reference
```bash
# Build and test
mvn clean install

# Run application
mvn spring-boot:run

# Run tests with coverage
mvn clean test jacoco:report

# Format code
mvn spotless:apply

# Check code style without changing
mvn spotless:check

# Generate application properties hint
mvn spring-boot:build-info

# Build Docker image (if docker-maven-plugin configured)
mvn clean package dockerfile:build

# Run specific test
mvn test -Dtest=UserServiceTest

# Debug mode
mvn spring-boot:run -Dspring-boot.run.arguments="--debug"
```

## When You Get Stuck
- Check the application logs first: look in `target/` or console output
- Common issues: port already in use (change in `application.properties`), database migrations failed (check Flyway scripts)
- For database issues: check entity mappings, lazy loading vs eager loading
- For API issues: check validation annotations, RequestBody/PathVariable mappings
- For test failures: check mocking setup, transaction handling in tests

## Current Known Issues / TODOs
- None yet - add as you find them

---

**Last Updated**: June 2026
**Team**: Add your team name here