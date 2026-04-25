# Sub-Agents & Tooling Registry

Snapshot of the runtime, dependencies, and tooling for the WorkLog backend.
All facts here are confirmed against `pom.xml`, `Docker/`, `src/main/resources/`,
and the source tree under `src/main/java/br/com/luizbrand/worklog/`.

## Active Sub-Agents

No project-specific sub-agent definitions in this repository. The harness can
delegate to its generic agents (`general-purpose`, `Explore`, `Plan`,
`code-reviewer`) when needed.

## MCPs / External Services

No MCP servers configured for this repository.

## Tech Stack

### Runtime & language
- Java 17 (`<java.version>17</java.version>` in `pom.xml`)
- Build: Maven Wrapper (`./mvnw`, `./mvnw.cmd`)

### Framework
- Spring Boot **3.5.9** (parent POM)
- Starters in use: `spring-boot-starter-data-jpa`, `-security`, `-validation`,
  `-web`, `-data-redis`, `-test`
- Spring Security: `@EnableWebSecurity`, `@EnableMethodSecurity(prePostEnabled = true)`,
  `RoleHierarchy` (`ADMIN` implies `USER`), stateless session policy

### Persistence
- PostgreSQL driver `org.postgresql:postgresql` (runtime scope) — image
  `postgres:17` in Docker Compose
- H2 (`com.h2database:h2`, test scope) — `jdbc:h2:mem:testdb`
- Flyway: `flyway-core` + `flyway-database-postgresql` — migrations live in
  `src/main/resources/db/migration/` (versions present: V1, V2, V3, V4, V5,
  V7, V8, V9, V10 — V6 is intentionally absent)
- JPA `ddl-auto`: `validate` in dev/prod, `create-drop` in test
- Redis (`spring-boot-starter-data-redis`) — image `redis:7.4-alpine`,
  protected by `--requirepass`. Used for refresh tokens with TTL.

### Libraries
- MapStruct **1.6.3** (annotation processor, `componentModel = "spring"`)
- Lombok (optional, annotation processor; `@Getter`, `@Setter`, `@SuperBuilder`,
  `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)
- Auth0 java-jwt **4.5.0** (token issuance and verification)
- springdoc-openapi-starter-webmvc-ui **2.8.6** (Swagger UI at `/swagger-ui`,
  spec at `/v3/api-docs`)
- spring-security-test (test scope)

### Local infrastructure
- `Docker/docker-compose.yaml` — services `postgres` and `redis`
- `.env` provides: `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `DB_PORT`,
  `REDIS_PORT`, `REDIS_PASSWORD`, `JWT_SECRET_KEY`, `JWT_EXPIRATION`,
  `JWT_REFRESH_EXPIRATION`

### Spring profiles
- Default profile: `dev` (set in `application.yaml`)
- `dev` (`application-dev.yaml`) — local PostgreSQL on `localhost:5432`,
  `show-sql: true`, Flyway enabled
- `prod` (`application-prod.yaml`) — `DB_PROD_URL/USER/PASS`, `show-sql: false`
- `test` (`application-test.yaml`) — H2 in-memory, `ddl-auto: create-drop`,
  Flyway disabled

### Build & test commands
- `./mvnw spring-boot:run` — runs the app (default `dev` profile)
- `./mvnw test` — full JUnit 5 + Mockito + AssertJ + Spring Security Test suite
- `./mvnw clean compile` — compile, including Lombok / MapStruct generation
- `./mvnw clean package` — build the jar
- `cd Docker && docker compose up -d` — bring up Postgres + Redis before running
- `spring-boot-maven-plugin` excludes Lombok from the packaged jar
- `maven-compiler-plugin` configures `annotationProcessorPaths` for Lombok and MapStruct

### Test patterns in the repo
- Unit tests: JUnit 5 + Mockito + AssertJ
- Mapper tests: `Mappers.getMapper(X.class)` (no `@SpringBootTest`)
- Controller tests: `@WebMvcTest` + `@AutoConfigureMockMvc(addFilters = false)`
  + `@MockitoBean AuthFilter` + `@MockitoBean CustomUserDetailsService`
- Shared builders under `src/test/java/.../support/`:
  `ClientTestBuilder`, `JwtTestSupport`, `RefreshTokenTestBuilder`,
  `RoleTestBuilder`, `SystemTestBuilder`, `TicketTestBuilder`, `UserTestBuilder`
- Test files in repository: 21 `*Test.java` classes plus 7 builders/support
  classes (29 `.java` files total under `src/test/`)

## Repository tooling
- `.githooks/pre-commit` — local pre-commit hook (executable; not auto-installed)
- `.claude/settings.json`, `.claude/settings.local.json` — Claude Code harness config
- `.claude/mvp-v1-plan.md` — current MVP v1 phased plan (Phases 1 and 2 already shipped)

## Forbidden / Required Patterns (from `CLAUDE.md`)
- Constructor injection only (no `@Autowired`, no `@RequiredArgsConstructor` on services/controllers)
- Never expose entities; always go through DTO records
- Never expose internal `id` (Long); only `publicId` (UUID) crosses the API boundary
- Schema changes only via new Flyway migrations — never edit applied ones, never `ddl-auto: create/update`
- PATCH semantics: nullable request fields, mappers use `NullValuePropertyMappingStrategy.IGNORE`
- Public endpoints: only `/worklog/auth/**` and Swagger (`/swagger-ui/**`, `/v3/api-docs/**`)
- Controller tests under `addFilters = false` must populate `SecurityContextHolder` directly when relying on `@AuthenticationPrincipal` (the `user(...)` post-processor does not work in that setup)
- TDD is mandatory: tests first, watch them fail, implement until green; full suite must be green before any commit
