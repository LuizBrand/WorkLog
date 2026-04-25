# CLAUDE.md — WorkLog Backend

## What is this project

WorkLog is a web app for support teams to register tasks they've already handled, ongoing tasks, or things waiting on development. Think of it as a shared log where the team records every customer interaction — what happened, what was done, and what's still pending.

The backend is a **Spring Boot REST API** with JWT auth, PostgreSQL, and Redis.

## Tech stack

- Java 17, Spring Boot 3.5.9
- PostgreSQL 17 (via Docker)
- Redis 7.4 (refresh token storage with TTL)
- Flyway (migrations in `src/main/resources/db/migration/`)
- MapStruct (DTO mapping)
- Lombok (`@Getter`, `@Setter`, `@SuperBuilder`, `@Builder`)
- Auth0 java-jwt (JWT tokens)
- H2 (test profile)

## Project structure

The project is organized by **module** (domain), not by technical layer. Each module contains its own Controller, Service, Repository, Mapper, DTOs, and Entity:

```
src/main/java/br/com/luizbrand/worklog/
├── auth/                  # Login, register, JWT, refresh token
│   ├── refreshtoken/      # RefreshToken entity (Redis), service, repository
│   └── dto/               # LoginRequest, RegisterRequest, AuthenticationResponse, etc.
├── client/                # Client CRUD + filtering with JPA Specification
│   └── dto/               # ClientRequest, ClientResponse, ClientSummary, ClientFiltersParams
├── config/                # SecurityConfig, DataInitializer
├── exception/             # Custom exceptions organized by HTTP status
│   ├── Business/          # BusinessException, RefreshTokenException → 422/401
│   ├── Conflict/          # ResourceAlreadyExistsException and children → 409
│   └── NotFound/          # ResourceNotFoundException and children → 404
├── exceptionhandler/      # RestExceptionHandler (@RestControllerAdvice), ApiExceptionResponse
├── role/                  # Role entity, repository, mapper, RoleName enum (ADMIN, USER)
├── shared/                # BaseEntity (abstract superclass)
├── system/                # System/product CRUD
│   └── dto/
├── tickets/               # Ticket CRUD + audit log (TicketLog, TicketLogManager)
│   ├── dto/               # TicketRequest, TicketUpdateRequest, TicketResponse
│   └── enums/             # TicketStatus, FieldType
└── user/                  # User CRUD + deactivation
    └── dto/
```

## Conventions to follow

### Module pattern

Every module follows the same structure. When adding a new module, create all files in the same package:
- `Entity.java` — JPA entity extending `BaseEntity`
- `EntityRepository.java` — Spring Data JPA interface
- `EntityService.java` — business logic
- `EntityController.java` — REST endpoints
- `EntityMapper.java` — MapStruct mapper (abstract class or interface, `componentModel = "spring"`)
- `dto/` — records for request/response objects

### BaseEntity

All entities extend `shared/BaseEntity.java` which provides:
- `id` (Long, auto-generated) — internal only, never exposed in APIs
- `publicId` (UUID, generated via `@PrePersist`) — used in all API paths and responses
- `createdAt`, `updatedAt` (auto-managed by Hibernate)
- `isEnabled` (Boolean, default true) — soft delete flag

Uses `@SuperBuilder` from Lombok, so entities must also use `@SuperBuilder`.

### DTOs

- DTOs are Java **records** (immutable)
- Request records use Jakarta Validation annotations (`@NotBlank`, `@NotNull`, `@Size`, `@Email`)
- Use separate Request and Response records — never expose entities directly
- PATCH semantics: update requests have all fields nullable, only non-null fields are applied
- Mappers use `NullValuePropertyMappingStrategy.IGNORE` for partial updates

### Dependency injection

Constructor injection only — no `@Autowired`. Lombok is NOT used for constructors in services/controllers; they are written manually.

### Exception handling

Custom exceptions inherit from two base classes:
- `ResourceNotFoundException` (extends `RuntimeException`) → 404
- `ResourceAlreadyExistsException` (extends `RuntimeException`) → 409
- `BusinessException` (extends `RuntimeException`) → 422
- `RefreshTokenException` (extends `BusinessException`) → 401

To add a new exception: create it under the appropriate `exception/` subpackage, extend the matching base class. The global `RestExceptionHandler` catches by base class, so new children are handled automatically.

### Naming

- Entities: singular (`Client`, `Ticket`, `User`). Exception: `Systems` (to avoid conflict with `java.lang.System`)
- Repositories: `findByPublicId(UUID)` is the standard lookup method
- Services: `findActiveEntity(UUID)` checks `isEnabled` and throws `BusinessException` if inactive
- Controller paths: plural nouns (`/clients`, `/tickets`, `/systems`, `/users`), except auth (`/worklog/auth`)

### Security

- JWT stateless auth configured in `config/SecurityConfig.java`
- Public endpoints: `/worklog/auth/**` only
- All other endpoints require authentication
- Roles: `ADMIN`, `USER` with hierarchy (ADMIN implies USER)
- `AuthFilter` extracts Bearer token from Authorization header
- Refresh tokens stored in Redis with TTL expiration

### Database

- Flyway manages all schema changes — never use `ddl-auto: create/update`
- JPA is set to `ddl-auto: validate`
- Migration files: `src/main/resources/db/migration/V{number}__{description}.sql`
- Entities use `@ManyToMany` with explicit join tables, `@ManyToOne` with FK columns

### Comments

- Code comments are written in Portuguese (pt-BR) — this is intentional
- Some exception messages mix Portuguese and English — follow the existing pattern in each module

### Testing (TDD)

- **Tests first, always.** Before writing or modifying any function/method/behavior, write the test(s) that describe the expected behavior, watch them fail, then implement until they pass.
- This applies to new features, bug fixes, and refactors that change observable behavior.
- **The full suite must be green before a change is considered done.** Run `./mvnw test` after any new function or alteration; do not commit or report a task complete while tests are failing or skipped.
- Unit tests use JUnit 5 + Mockito + AssertJ. Mapper tests use `Mappers.getMapper(X.class)` (not `@SpringBootTest`). Controller tests use `@WebMvcTest` with `@AutoConfigureMockMvc(addFilters = false)` and must declare `@MockitoBean AuthFilter` + `@MockitoBean CustomUserDetailsService`.
- Any controller test relying on `@AuthenticationPrincipal` must populate `SecurityContextHolder` directly in `@BeforeEach` (and clear in `@AfterEach`) — `SecurityMockMvcRequestPostProcessors.user(...)` does not work with `addFilters = false`.

## Role

You are a tactical executor working under a human owner. Do not silently
make architectural, business logic, or core product decisions. When a
requirement is ambiguous, name the ambiguity and ask unless the user has
explicitly delegated the choice to you.

Default priorities:

1. Correctness
2. Maintainability
3. Minimal surface area
4. Speed

---

## Persistent State

Chat history is not durable memory. On session start, read these files
when they exist. As work progresses, keep them accurate.

- `memory/agents.md` — active agents, MCPs, tech stack, tooling
- `memory/plan.md` — macro design and vertical slices
- `memory/progress.md` — atomic task checklist and current status
- `memory/verify.md` — definition of done and required checks
- `memory/gotchas.md` — mistakes already corrected by the human

If the files do not exist, initialize them before substantive work.

---

## Planning

Use a written plan for non-trivial work: multi-file changes, behavioral
changes, architectural choices, or anything that needs more than a small
obvious edit.

Plan in this order:

1. **Context** — map the relevant code and existing patterns.
2. **Questions** — surface ambiguous requirements and tradeoffs.
3. **Structure** — update `memory/plan.md` and `memory/verify.md`.
4. **Tasks** — add atomic steps to `memory/progress.md`.
5. **Execution** — implement the next bounded slice.

For obvious one- or two-line fixes, execute directly and verify.

When asked only to plan, output the plan and do not edit code. When the
user approves a plan, execute without repeating it.

---

## Execution Limits

- Execute one small vertical slice at a time.
- Avoid broad refactors mixed with feature work.
- Keep a phase to roughly five touched files unless the change is purely mechanical.
- For large independent areas, split the work and verify each area separately.

---

## User Intent And Control

- If the user provides a written plan, follow it step by step. Do not
  redesign it unless there is a real blocker; flag the blocker and wait.
- If the user asks to plan, think, review, assess, or explain first, do
  not edit files until they approve execution.
- Never push to a shared remote unless the user explicitly asks.
- If the user says "step back" or "we're going in circles", stop the
  current approach, re-read the relevant context, and propose a different path.
- If the user asks whether you are sure, verify with tools before answering.
- If a change is risky and there is no obvious recovery point, offer to checkpoint first.

---

## Self-Correction

- After any correction from the human, add the pattern to `memory/gotchas.md`.
- If a fix fails twice, stop and re-read the relevant code top-down.
  State what assumption was wrong before trying again.
- When asked to test your own output, use a new-user path through the
  feature, not just code inspection.

---

## Context Management

- After long conversations, re-read relevant files before editing.
- If memory is degrading, write the current state to `memory/progress.md`
  before compacting or handing work off.
- For large files, read focused chunks instead of relying on one huge output.
- If tool output is truncated, read the saved full output or rerun a
  narrower command before acting.

## How to run

```bash
cd Docker && docker compose up -d   # starts PostgreSQL + Redis
./mvnw spring-boot:run              # runs with dev profile
```

Profiles: `dev` (default, local PostgreSQL), `prod` (env vars), `test` (H2 in-memory).
