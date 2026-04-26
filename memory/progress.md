# Atomic Progress Log

Tick atomic tasks as they complete. Never mark a task done unless the
criteria in `memory/verify.md` are satisfied. The `state-enforcement.sh`
hook (configured via `.claude/`) blocks completion when source files
changed without an update here.

## In Progress
- [ ] MVP Phase 5 (`DELETE /tickets/{publicId}` soft delete + `StatusFiltro visibility` filter, ADMIN-only) — code complete, suite green at 195/195, awaiting user approval to commit (`feat(tickets): add soft-delete and admin-only deleted visibility`).

## Session log
- [x] 2026-04-25 — MVP Phase 5 implemented TDD. Phase 6 (default sort) discarded per user direction; cleanup applied to `.claude/mvp-v1-plan.md`, `memory/plan.md`, `memory/progress.md`. Naming-collision resolved by user decision: new field is `StatusFiltro visibility` on `TicketFiltersParams` (not `status`), wire param `?visibility=ATIVO|INATIVO|TODOS` — keeps existing `?status=PENDING` (TicketStatus) intact.
  - Tests first: extended `TicketSpecificationTest` (+3 visibility cases), `TicketServiceTest` (+5 FindAllTickets visibility/role-gating cases incl. helper, +2 SoftDeleteTicket cases), `TicketControllerTest` (+2 DeleteTicket cases, principal+visibility round-trip in FindAllTickets). Watched compile fail naming missing `findAll(.., User)` and `softDeleteTicket`, then watched 4 service tests fail on assertion before role gating.
  - Production: `TicketFiltersParams.visibility` added (8th field). `TicketSpecification` adds `equal(isEnabled, true|false)` predicate per visibility. `TicketService.findAll(filters, pageable, currentUser)` computes effective visibility — non-ADMIN forced to `ATIVO`, ADMIN preserves caller, ADMIN null defaults to `ATIVO`. `TicketService.softDeleteTicket(UUID)` (404 + setIsEnabled(false) + save). `TicketController#findAllTickets` gains `@AuthenticationPrincipal`; new `DELETE /tickets/{publicId}` with `@PreAuthorize("hasRole('ADMIN')")` returns 204. `TicketControllerDocs` updated for both.
  - Suite: 195/195 green (183 → 195, +12 = 3 spec + 7 service + 2 controller).
  - Awaiting user approval to commit `feat(tickets): add soft-delete and admin-only deleted visibility` (no Co-Authored-By trailer).
- [x] 2026-04-25 — initialized `memory/` snapshot of current project state (agents, plan, progress, verify, gotchas). Committed as `b8c02da` `docs(memory): seed agent memory snapshot`. Working-tree change to `.gitignore` is pre-existing from before this session and not part of this commit.
- [x] 2026-04-25 — MVP Phase 3 (`GET /users/me`) implemented TDD. Added `UserControllerTest$getMe` (2 tests: 200 OK + principal-passthrough), `UserService.getMe(User)`, `GET /users/me` on `UserController` with `@AuthenticationPrincipal`, and matching `UserControllerDocs#getMe` operation. Suite at 175/175 green. Committed as `0b65888` `feat(users): add GET /users/me endpoint`.
- [x] 2026-04-25 — MVP Phase 4 (`POST /users/me/change-password`) implemented TDD per `.claude/mvp-v1-phases-4-5.md`:
  - Wrote 4 failing service tests (`UserServiceTest$ChangeMyPassword`: happy path, wrong current password → `BusinessException`, refresh token missing → `BusinessException`, refresh token belongs to other user → `BusinessException`) and 4 failing controller tests (`UserControllerTest$ChangeMyPassword`: 204 success, 422 on `BusinessException`, 400 on validation, principal-passthrough). Confirmed compilation failures named the missing `ChangePasswordRequest`, then implemented.
  - New DTO `ChangePasswordRequest` (record with `@NotBlank currentPassword`, `@NotBlank @Size(min=8) newPassword`, `@NotBlank refreshToken`).
  - `UserService` ctor now also takes `@Lazy PasswordEncoder` + `RefreshTokenService`; new `changeMyPassword(User, ChangePasswordRequest)` validates current password, validates refresh-token ownership against `currentUser.getEmail()`, re-hashes and saves the user, then deletes the supplied refresh token.
  - `UserController` exposes `POST /users/me/change-password` returning 204; `UserControllerDocs#changeMyPassword` documents 204/400/401/422.
  - **Gotcha caught by full suite only:** non-lazy `PasswordEncoder` injection produced `BeanCurrentlyInCreationException` (`UserService` → `SecurityConfig` → `AuthFilter` → `CustomUserDetailsService` → `UserService`). Fixed with `@Lazy` on the ctor param; logged in `memory/gotchas.md`. Lesson: focused `UserServiceTest`/`UserControllerTest` did not boot the full context — only `WorklogApplicationTests` did.
  - Aligned `ChangePasswordRequest#newPassword` validation with the existing `RegisterRequest#password` rule (`@NotBlank` + `@Size(min=8)` + `@Pattern("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$")` with the same Portuguese messages) per user request — keep `currentPassword` and `refreshToken` as plain `@NotBlank`. Updated the controller happy-path fixture to `NewStrong1Password` so it satisfies the new pattern.
  - User explicitly chose to keep the `@Lazy PasswordEncoder` workaround instead of extracting `PasswordEncoder` into its own `@Configuration`. Decision recorded; no architectural change to `SecurityConfig`.
  - Suite: 183/183 green (175 → 183, +8 = 4 service + 4 controller).
  - Shipped as `be684bc` `feat(users): add POST /users/me/change-password`; memory commit `65294ab`.

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

### MVP v1 — Phase 3 (shipped — commit `0b65888`)
- [x] `UserControllerTest$getMe` — 2 tests written first (TDD), failed for the right reason (`getMe` symbol missing), then turned green
- [x] `UserService.getMe(User)` added (delegates to `userMapper.toUserResponse`)
- [x] `GET /users/me` in `UserController` with `@AuthenticationPrincipal User currentUser`
- [x] `UserControllerDocs#getMe` documented (200 + 401)
- [x] `./mvnw test` green — 175/175
- [x] Commit `feat(users): add GET /users/me endpoint` → `0b65888`

### MVP v1 — Phase 5 (in flight, per `.claude/mvp-v1-phases-4-5.md`)
- [ ] Phase 5 — `DELETE /tickets/{publicId}` (soft delete, ADMIN-only) + `StatusFiltro visibility` filter on `GET /tickets` (non-ADMIN forced to `ATIVO`; ADMIN default `ATIVO` when `null`)

### Discarded
- Phase 6 — `GET /tickets` default sort `updatedAt DESC` — dropped per user direction (2026-04-25)

### Test coverage track
- [ ] Confirm with the user whether `.claude/test-plan.md` (referenced in user-level memory) still applies, or if it was folded into `.claude/mvp-v1-plan.md`
- [ ] If still active, identify the next module to cover — the global memory says "Phase 5 = system module", but `system` already has mapper/service/controller tests; clarify before planning more

## Blocked
- [ ] Test-coverage plan at `.claude/test-plan.md` — file not found in this repository (only `.claude/mvp-v1-plan.md` is present). Awaiting clarification.
