# Atomic Progress Log

Tick atomic tasks as they complete. Never mark a task done unless the
criteria in `memory/verify.md` are satisfied. The `state-enforcement.sh`
hook (configured via `.claude/`) blocks completion when source files
changed without an update here.

## In Progress
- Backend gaps plan at `.claude/backend-gaps-implementation-plan.md` — Phases 1 (`840813a`), 2 (`34b49f9`), 3 (`98431e5`), 4 (`be1b843`), 5 (`8e1d98e`), 6 (`6baa088`), 7a (`94c744c`), 7b (`598e346`) shipped. Phase 7c (CORS for credentialed cookies + cookie yaml) still queued.

## Session log
- [x] 2026-05-10 — Backend gaps Phase 7b (AuthFilter reads JWT from `worklog_access` cookie) implemented TDD; shipped as `598e346`.
  - `AuthFilter.doFilterInternal` now reads `jwt` from `request.getCookies()`, scanning for the `worklog_access` cookie (hardcoded constant `ACCESS_COOKIE_NAME = "worklog_access"` per plan). Authorization header branch removed entirely. Helper `readAccessCookie(HttpServletRequest)` returns null when cookies are absent, the named cookie is missing, or the value is blank.
  - Tests first: `AuthFilterTest` rewritten — 6 tests under `doFilterInternal()`: no cookies, refresh-cookie-only (no access cookie), valid access cookie (auth populated), unparseable token, `isTokenValid=false`, **plus** new `shouldIgnoreAuthorizationHeader` that asserts a Bearer header alone (no cookies) does **not** authenticate (confirms header path is gone). Confirmed initial run red on the cookie-based tests (filter still hit header branch), then implementation flipped the suite green.
  - Suite: 234/234 green (233 → 234, +1 net = the new `shouldIgnoreAuthorizationHeader` test).
  - Shipped as `598e346` `feat(auth): authenticate from access cookie`.

- [x] 2026-05-10 — Backend gaps Phase 7a (emit auth tokens via HttpOnly cookies) implemented TDD; shipped as `94c744c`.
  - New types: `auth/CookieProperties` (record, `@ConfigurationProperties("worklog.cookies")` with `@DefaultValue` for `secure=false`, `sameSite=Strict`, `accessName=worklog_access`, `refreshName=worklog_refresh`, `refreshPath=/worklog/auth`); `auth/AuthCookieService` (builds/clears access & refresh `ResponseCookie`s; access TTL from `TokenProperties.expiration`, refresh TTL from `TokenProperties.refreshToken.expiration`); `auth/AuthTokens` (internal record returned by `AuthService.login`/`refreshToken`).
  - Wired `@EnableConfigurationProperties(CookieProperties.class)` on `SecurityConfig` (CorsProperties uses the same pattern via `CorsConfig`).
  - `AuthController` rewritten: `login` returns 204 + `Set-Cookie: worklog_access` (path `/`) + `Set-Cookie: worklog_refresh` (path `/worklog/auth`); `refresh` reads `@CookieValue(name="worklog_refresh", required=false)` and throws `RefreshTokenException` when null → 401 via global handler; `logout` reads same cookie, calls service only if present (idempotent), always clears both cookies via `Set-Cookie: ... Max-Age=0`. `register` unchanged (still returns 201 + `RegisterResponse`).
  - `AuthService.login` / `refreshToken` now return `AuthTokens` instead of the deleted `AuthenticationResponse`.
  - `AuthControllerDocs` updated to advertise the cookie-based flow.
  - **Deleted:** `auth/dto/AuthenticationResponse.java`, `auth/refreshtoken/RefreshTokenRequest.java`.
  - Tests first: `AuthControllerTest` rewritten — `LoginEndpoint.shouldReturn204AndSetCookiesOnSuccess` asserts HttpOnly/path/value for both cookies + `SameSite=Strict` substring on the `Set-Cookie` header. `RefreshEndpoint` has 3 cases (success rotates both cookies, missing cookie → 401, invalid token → 401). `LogoutEndpoint` has 2 cases (with cookie: delegates + clears; without cookie: still clears + no service call). `AuthServiceTest` updated to expect `AuthTokens` return type with `accessToken()`/`refreshToken()` accessors. Watched test-compile fail (missing `AuthCookieService`, `AuthTokens`), then implemented.
  - `application.yaml` / `application-dev.yaml` / `application-prod.yaml` **not** changed — yaml block (and `prod.secure=true` / `dev.secure=false`) is intentionally deferred to Phase 7c per the plan. App boots fine because `CookieProperties` uses `@DefaultValue` on every component.
  - Suite: 233/233 green (231 → 233, +2 net = 1 new refresh missing-cookie test + 1 new logout-without-session test; existing login/refresh/logout tests rewritten for cookies, register tests untouched).
  - Shipped as `94c744c` `feat(auth): emit tokens via HttpOnly cookies`.

- [x] 2026-05-10 — Backend gaps Phase 6 (`DELETE /clients/{publicId}` admin soft-delete) implemented TDD. Mirrors `TicketController.deleteTicket` (`TicketController.java:40-45`) + `TicketService.softDeleteTicket` (`TicketService.java:93-99`). New `ClientService.softDeleteClient(UUID)` (`@Transactional`, finds by publicId or `ClientNotFoundException`, sets `isEnabled=false`, saves). New `ClientController.softDeleteClient` (`@DeleteMapping("/{publicId}")` + `@PreAuthorize("hasRole('ADMIN')")` returning 204). Added Swagger op in `ClientControllerDocs` documenting 204/401/403/404.
  - Tests first: `ClientServiceTest$SoftDeleteClient.shouldSoftDeleteWhenClientExists` + `shouldThrowWhenMissing`; `ClientControllerTest$DeleteClient.shouldReturn204OnSuccess` + `shouldReturn404WhenMissing`. Watched test-compile fail (missing `ClientService.softDeleteClient`), then implemented.
  - Suite: 231/231 green (227 → 231, +4 = 2 service + 2 controller).
  - `backend-gaps.md` Gap 6 removed in the same commit.
  - Shipped as `6baa088` `feat(clients): add admin soft-delete endpoint`.

- [x] 2026-05-10 — Backend gaps Phase 5 (`enabled` em `ClientRequest` PATCH) implemented TDD. Added `Boolean enabled` (3rd positional, nullable) to `ClientRequest` record. `ClientMapper.updateClient` now carries `@Mapping(source = "clientRequest.enabled", target = "isEnabled")` — `@BeanMapping(NullValuePropertyMappingStrategy.IGNORE)` keeps null PATCH bodies non-overwriting.
  - Tests first: `ClientControllerTest$updateClient.shouldReturnDisabledWhenPatchSetsEnabledFalse` (PATCH `enabled=false` → JSON `enabled=false`); `ClientServiceTest$UpdateClient.shouldForwardEnabledFalseToMapper` (request with `enabled=false` is forwarded to mapper); `ClientServiceTest$ClientMapperTest$UpdateClient.shouldFlipIsEnabledWhenEnabledFalse` + `shouldEnableIsEnabledWhenEnabledTrue` (mapper actually mutates `client.isEnabled`). Watched test-compile fail for the right reason (3-arg ctor missing), then implemented.
  - Adjusted 12 `new ClientRequest(...)` call sites across `ClientControllerTest` and `ClientServiceTest` to pass the 3rd `enabled` arg.
  - **Pre-existing rot fixed (collateral, in-scope by necessity):** the inner `ClientServiceTest$ClientMapperTest` was declared `static` (not `@Nested`), so JUnit 5 silently skipped all 5 mapper tests inside it (`shouldMapToClient`, `shouldMapToClientResponse`, `shouldReturnNullWhenClientIsNull`, `shouldUpdateClient`, `shouldNotUpdateClientWithNulls`). Without the conversion, my 2 new mapper tests would have been dead too. Converted the inner class to `@Nested` (drops `@ActiveProfiles`/`@ExtendWith` inherited from outer; adds `@DisplayName("ClientMapper")`). **Gotcha to remember: a `static` inner test class in JUnit 5 is silently dead — only `@Nested` non-static is auto-discovered.**
  - Suite: 227/227 green (218 → 227, **+9** = 2 nested-class new mapper tests for Phase 5 + 5 pre-existing tests resurrected by the `@Nested` fix + 1 service test + 1 controller test).
  - `backend-gaps.md` Gap 5 removed in the same commit.
  - Shipped as `8e1d98e` `feat(clients): allow toggling enabled in update`.

- [x] 2026-05-10 — Backend gaps Phase 4 (`enabled` em `SystemResponse`) implemented TDD. Added `boolean enabled` (3rd positional) to `SystemResponse` record; `SystemMapper.toSystemResponse` carries `@Mapping(source="isEnabled", target="enabled")` mirroring `ClientMapper:26`. Tests first: `SystemMapperTest.ToSystemResponseTests` adds `shouldMapEnabledTrue` + `shouldMapEnabledFalse` (and existing `shouldMapSystemToResponse` now asserts `enabled=true`); `SystemControllerTest.FindSystemByPublicId` adds `shouldExposeDisabledFlag` (and existing `shouldReturnSystemWhenFound` now asserts JSON `$.enabled=true`). Confirmed test-compile failed for the right reason (3-arg ctor missing, `enabled()` accessor missing) before implementation. Adjusted 9 `new SystemResponse(...)` call sites across `SystemControllerTest`, `SystemServiceTest`, `SystemMapperTest`, `ClientControllerTest`, `TicketControllerTest`, `TicketMapperTest` to pass the 3rd `enabled` arg.
  - Suite: 218/218 green (215 → 218, +3 net = 2 mapper + 1 controller).
  - Read-only feature: PATCH editing of `enabled` for Systems explicitly out of scope per plan.
  - `backend-gaps.md` Gap 4 removed in the same commit.
  - Shipped as `be1b843` `feat(systems): expose enabled flag in response`.

- [x] 2026-05-10 — Backend gaps Phase 3 (`TicketPriority` field) implemented TDD. New `TicketPriority` enum (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`); `Ticket` entity carries `@Enumerated(STRING) @Column(nullable=false, length=20) priority`; `TicketRequest` adds `@NotNull priority`; `TicketUpdateRequest` adds nullable `priority` (PATCH); `TicketResponse` and `TicketSummary` expose `priority`; `TicketService.prepareNewTicket` and `updateTicketEntity` apply priority with PATCH semantics; `TicketLogManager.generateLogs` adds a `"priority"` STRING checkChange. Flyway migration `V11__add_priority_to_tickets.sql` adds the column nullable, backfills `MEDIUM`, then `SET NOT NULL`. `TicketTestBuilder` defaults priority to `MEDIUM` and exposes `withPriority(...)`.
  - Tests first: behavior assertions (`shouldPersistPriorityOnCreate`, `shouldUpdatePriorityWhenProvided`, `shouldPreservePriorityWhenNullOnUpdate` in service; `shouldLogPriorityChange` in log manager; `shouldReturn400WhenPriorityMissingOnCreate` in controller; `priority` field asserted in `TicketMapperTest` toEntity/toResponse/toSummary plus controller GET-by-publicId and list-summaries). Adjusted every positional `new TicketRequest(...)` and `new TicketUpdateRequest(...)` site (12 ctor calls across 3 test files) to add the new arg.
  - Test profile (H2 + `ddl-auto: create-drop`, Flyway disabled) auto-derives the priority column from the entity annotation, so the migration is dev/prod-only — local suite covers behavior, not the migration itself. (Note: the test YAML still says H2; user mentioned the actual test DB is Postgres — YAML may be stale, flagged as a follow-up cleanup.)
  - Suite: 215/215 green (210 → 215, +5).
  - V11 confirmed in dev: user booted via IntelliJ after a `docker compose down -v && up` to reset the postgres volume (existing volume had stale credentials from earlier init).
  - Shipped as `98431e5` `feat(tickets): add priority field`.

- [x] 2026-05-10 — Backend gaps Phase 2 (`TicketStatus.CANCELLED`) implemented TDD. Tests first: `TicketLogManagerTest.shouldLogStatusTransitionToCancelled`, `TicketServiceTest.shouldPersistCancelledStatusOnCreate`, `TicketServiceTest.shouldPersistCancelledStatusOnUpdate`, `TicketControllerTest.shouldAcceptCancelledStatusOnCreate`, `TicketControllerTest.shouldAcceptCancelledStatusOnUpdate`. Compile failed for the right reason (missing enum constant), then implementation appended `CANCELLED` to `TicketStatus`. No migration: `tickets.status` is `VARCHAR(50) NOT NULL` (V1) and Hibernate persists the enum name as-is. `backend-gaps.md` Gap 2 removed in the same slice.
  - Suite: 210/210 green (205 → 210, +5).
  - Shipped as `34b49f9` `feat(tickets): support CANCELLED status`.

- [x] 2026-05-10 — Backend gaps Phase 1 (`userId` reassignment in `TicketUpdateRequest`) implemented TDD. Tests first: 3 new `TicketServiceTest$UpdateTicketTests` cases (reassign / keep when null / propagate `BusinessException` when reassigned user inactive), 1 new `TicketLogManagerTest` case (logs `"user"` STRING field as old-email → new-email), 1 new `TicketControllerTest$UpdateTicket` case (forwards `userId` payload to service). Also fixed 4 compile-broken `TicketUpdateRequest` constructor calls (record now 6 args after `userId` was added) in `TicketServiceTest` and `TicketControllerTest`. Watched red on the four behavioral tests, then implemented.
  - `TicketUpdateRequest` adds `UUID userId`.
  - `TicketService.updateTicket` resolves the reassigned user **once** via `userService.findActiveUser(...)` before logging/saving; passes the resolved `User` (or `null`) to `prepareNewTicket` and `updateTicketEntity` to avoid double DB lookups. `BusinessException` from inactive reassignment propagates before `ticketLogManager.generateLogs` and `ticketRepository.save` (verified by tests).
  - `TicketLogManager.generateLogs` adds a 6th `checkChange` for `"user"` (`FieldType.STRING`) using `User#getEmail()` on each side (null-safe).
  - `backend-gaps.md` Gap 1 removed (per plan: same commit as the feature). `backend-gaps.md` was untracked before this slice; this commit promotes it to a tracked source-of-truth file.
  - Suite: 205/205 green (200 → 205, +5).
  - Shipped as `840813a` `feat(tickets): allow user reassignment in update`.

- [x] 2026-04-26 — CORS slice for Next.js frontend implemented TDD. New `CorsProperties` (`@ConfigurationProperties("worklog.cors")`, record + `@DefaultValue` empty list) and `CorsConfig` (`@Configuration` with `CorsConfigurationSource` bean: `GET/POST/PUT/PATCH/DELETE/OPTIONS`, `Authorization/Content-Type/Accept`, `maxAge=3600`, **no `allowCredentials`** since auth is Bearer-token in the `Authorization` header). `SecurityConfig` adds `.cors(Customizer.withDefaults())` to pick the bean up. `application.yaml` ships `http://localhost:3000` as the shared default (covers `dev`/`test`); `application-prod.yaml` reads from `${WORKLOG_CORS_ORIGINS}` (Spring binds comma-separated → `List<String>`). Test class `config/CorsConfigTest.java` adds 5 unit cases on the bean directly (origins, methods, headers, no-credentials, multi-origin). Suite: 200/200 green (195 → 200, +5). Shipped as `fe9d8a6` `feat(security): enable CORS for the Next.js frontend`.
- [x] 2026-04-25 — MVP Phase 5 implemented TDD. Phase 6 (default sort) discarded per user direction; cleanup applied to `.claude/mvp-v1-plan.md`, `memory/plan.md`, `memory/progress.md`. Naming-collision resolved by user decision: new field is `StatusFiltro visibility` on `TicketFiltersParams` (not `status`), wire param `?visibility=ATIVO|INATIVO|TODOS` — keeps existing `?status=PENDING` (TicketStatus) intact.
  - Tests first: extended `TicketSpecificationTest` (+3 visibility cases), `TicketServiceTest` (+5 FindAllTickets visibility/role-gating cases incl. helper, +2 SoftDeleteTicket cases), `TicketControllerTest` (+2 DeleteTicket cases, principal+visibility round-trip in FindAllTickets). Watched compile fail naming missing `findAll(.., User)` and `softDeleteTicket`, then watched 4 service tests fail on assertion before role gating.
  - Production: `TicketFiltersParams.visibility` added (8th field). `TicketSpecification` adds `equal(isEnabled, true|false)` predicate per visibility. `TicketService.findAll(filters, pageable, currentUser)` computes effective visibility — non-ADMIN forced to `ATIVO`, ADMIN preserves caller, ADMIN null defaults to `ATIVO`. `TicketService.softDeleteTicket(UUID)` (404 + setIsEnabled(false) + save). `TicketController#findAllTickets` gains `@AuthenticationPrincipal`; new `DELETE /tickets/{publicId}` with `@PreAuthorize("hasRole('ADMIN')")` returns 204. `TicketControllerDocs` updated for both.
  - Suite: 195/195 green (183 → 195, +12 = 3 spec + 7 service + 2 controller).
  - Shipped as `63d5225` `feat(tickets): add soft-delete and admin-only deleted visibility`; plan/memory cleanup committed as `7058871` `docs(memory): record Phase 5 shipped, drop Phase 6`.
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
