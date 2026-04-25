# Atomic Progress Log

Tick atomic tasks as they complete. Never mark a task done unless the
criteria in `memory/verify.md` are satisfied. The `state-enforcement.sh`
hook (configured via `.claude/`) blocks completion when source files
changed without an update here.

## In Progress
- [ ] (none — waiting on next instruction from the user)

## Session log
- [x] 2026-04-25 — initialized `memory/` snapshot of current project state (agents, plan, progress, verify, gotchas). Committed as `b8c02da` `docs(memory): seed agent memory snapshot`. Working-tree change to `.gitignore` is pre-existing from before this session and not part of this commit.

## Completed (consolidated from current code state)

### Infrastructure & configuration
- [x] Maven project with Spring Boot 3.5.9 / Java 17 (`pom.xml`)
- [x] `Docker/docker-compose.yaml` with PostgreSQL 17 and Redis 7.4-alpine
- [x] Profiles `dev`, `prod`, `test` in `src/main/resources/application*.yaml`
- [x] `WorklogApplication` (`@SpringBootApplication`) entrypoint
- [x] `SecurityConfig`: stateless session, JWT filter chain, role hierarchy (`ADMIN` → `USER`), BCrypt encoder
- [x] `DataInitializer` seeds default roles (`ADMIN`, `USER`) at startup
- [x] `OpenApiConfig` + springdoc-openapi (Swagger UI at `/swagger-ui`, spec at `/v3/api-docs`); both whitelisted in `SecurityConfig`
- [x] Flyway migrations V1..V10 (V6 intentionally absent) under `src/main/resources/db/migration/`

### Shared layer
- [x] `shared/BaseEntity`: `id`, `publicId` (UUID via `@PrePersist`), `createdAt`, `updatedAt`, `isEnabled` defaulted to `true`
- [x] Exception families in `exception/{Business,Conflict,NotFound}/` mapped through `RestExceptionHandler` to `ApiExceptionResponse`

### `auth` module
- [x] `User` extends `BaseEntity` and implements `UserDetails`; `UserRepository`
- [x] `Role` + `RoleRepository` + `RoleMapper` + enum `RoleName`
- [x] `JwtService` (Auth0 java-jwt) + `TokenProperties`
- [x] `AuthFilter` extracts the Bearer token and populates the `SecurityContext`
- [x] `CustomUserDetailsService` loads users by email
- [x] `RefreshToken` (Redis hash with TTL) + `RefreshTokenRepository` + `RefreshTokenService`
- [x] `AuthService.register` (rejects duplicate emails)
- [x] `AuthService.login` (issues access + refresh tokens)
- [x] `AuthService.refreshToken` (rotates refresh token)
- [x] `AuthService.logout` (deletes refresh token from Redis)
- [x] `AuthController` at `/worklog/auth`: `register`, `login`, `refresh`, `logout`
- [x] DTOs: `RegisterRequest`, `RegisterResponse`, `LoginRequest`, `AuthenticationResponse`, `RefreshTokenRequest`

### `user` module
- [x] `UserService`: `findAll`, `findByPublicId`, `deactiveUser`, `findEntityByPublicId`, `findActiveUser`, `findUserByEmail`, `saveUser`
- [x] `UserController`: `GET /users/`, `GET /users/{publicId}`, `POST /users/{publicId}/deactivate` (ADMIN-only)
- [x] `UserMapper`, `UserResponse`, `UserSummary`

### `client` module
- [x] `Client` (with `@ManyToMany` to `Systems` via join table `client_system`)
- [x] `ClientService`: create, update, list with filters, lookup by publicId, `findActiveClient`
- [x] `ClientSpecification` for dynamic filtering (name, status, associated systems)
- [x] `ClientController`: `GET /clients`, `GET /clients/{publicId}`, `POST /clients/`, `PATCH /clients/{publicId}`
- [x] DTOs: `ClientRequest`, `ClientResponse`, `ClientSummary`, `ClientFiltersParams`, enum `StatusFiltro`
- [x] Duplicate detection via `ClientAlreadyExistsException`

### `system` module
- [x] `Systems` entity (renamed to avoid clashing with `java.lang.System`)
- [x] `SystemService`: create, update, list, lookup by publicId, `findActiveSystem`
- [x] `SystemController`: `GET /systems`, `GET /systems/{publicId}`, `POST /systems/`, `PATCH /systems/{publicId}`
- [x] Duplicate detection via `SystemAlreadyExistsException`

### `tickets` module
- [x] `Ticket` with FKs to `Client`, `Systems`, `User` and `TicketStatus` enum
- [x] `TicketLog` with `change_group_id`, FK to ticket/user, optional FKs to client/system, typed `field_type`, `old_value` / `new_value` TEXT columns
- [x] `TicketLogManager.generateLogs` diffs old vs new ticket field-by-field, batched by `changeGroupId`
- [x] `TicketService.createTicket`, `findTicketByPublicId`, `updateTicket` (with auditing and `@AuthenticationPrincipal` as the author)
- [x] **MVP Phase 1** — `TicketSpecification` + `TicketRepository extends JpaSpecificationExecutor` + `TicketService.findAll(filters, pageable)` returning `Page<TicketSummary>` (commit `a3a8b72`)
- [x] **MVP Phase 2** — `TicketLogManager.findLogsByTicket` + `GET /tickets/{publicId}/logs` paginated returning `Page<TicketLogResponse>` (commit `c812722`)
- [x] DTOs: `TicketRequest`, `TicketUpdateRequest`, `TicketResponse`, `TicketSummary`, `TicketFiltersParams`, `TicketLogResponse`
- [x] Enums: `TicketStatus` (`PENDING`, `AWAITING_CUSTOMER`, `AWAITING_DEVELOPMENT`, `COMPLETED`), `FieldType`

### Test suite
- [x] 21 `*Test.java` classes plus 7 shared builders/support classes under `src/test/java/.../support/` (29 `.java` files total under `src/test/`)
- [x] Coverage spans controllers, services, mappers, specifications, the auth filter, the refresh token service, and the global exception handler
- [x] `WorklogApplicationTests` smoke test for context loading

## Backlog (next up)

### MVP v1 — Phase 3 (next planned phase)
- [ ] `UserControllerTest$GetMe` — write tests first (TDD) for 200 OK with the principal's data
- [ ] Implement `GET /users/me` in `UserController`, returning `UserResponse` from `@AuthenticationPrincipal`
- [ ] (optional) Add `UserService.getMe(User)` for consistency with other endpoints
- [ ] `./mvnw test` green
- [ ] Commit `feat(users): add GET /users/me endpoint`

### MVP v1 — optional later phases (only after user confirmation)
- [ ] Phase 4 — `POST /users/me/change-password` + invalidation of refresh tokens
- [ ] Phase 5 — `DELETE /tickets/{publicId}` (soft delete) with `isEnabled = true` as the default list filter
- [ ] Phase 6 — `GET /tickets` defaulting to `Sort.by("updatedAt").descending()` when no sort supplied

### Test coverage track
- [ ] Confirm with the user whether `.claude/test-plan.md` (referenced in user-level memory) still applies, or if it was folded into `.claude/mvp-v1-plan.md`
- [ ] If still active, identify the next module to cover — the global memory says "Phase 5 = system module", but `system` already has mapper/service/controller tests; clarify before planning more

## Blocked
- [ ] Test-coverage plan at `.claude/test-plan.md` — file not found in this repository (only `.claude/mvp-v1-plan.md` is present). Awaiting clarification.
