# Macro Plan

The architectural design and the next vertical slices of work. Updated when
direction changes. All claims here come from the actual source tree, the
Flyway migrations, and `.claude/mvp-v1-plan.md`.

## What this project does

WorkLog is a Spring Boot REST backend for support teams to register customer
interactions: what happened, what was done, and what is still pending. It is
a shared post-interaction log, not an SLA-driven service desk. Users
authenticate, register tickets bound to a Client and a System/product,
update status as the case evolves, and every change is audited per field
through `TicketLog`.

## Architecture

- Stateless REST API. Auth via short-lived JWT access token (Auth0 java-jwt)
  + refresh token persisted in Redis with TTL.
- Organized **by module (domain)**, not by technical layer. Each module
  (`auth`, `client`, `role`, `system`, `tickets`, `user`) ships its own
  Controller, Service, Repository, Mapper, DTOs and Entity.
- Every domain entity extends `shared/BaseEntity`: internal `id` (Long),
  exposed `publicId` (UUID generated in `@PrePersist`), `createdAt`,
  `updatedAt`, and `isEnabled` (default `true`, soft-delete flag).
- DTOs are Java records. PATCH-style updates use nullable fields; mappers
  apply `NullValuePropertyMappingStrategy.IGNORE`.
- Schema is owned by Flyway (V1..V10, V6 absent on purpose). JPA stays in
  `validate` mode in dev/prod and `create-drop` in test (Flyway off in test).
- Custom exceptions are organized by HTTP status family in
  `exception/{Business,Conflict,NotFound}/` and mapped centrally by
  `RestExceptionHandler` to `ApiExceptionResponse`:
  - `ResourceNotFoundException` → 404
  - `ResourceAlreadyExistsException` → 409
  - `BusinessException` → 422 (subclass `RefreshTokenException` → 401)
- Dynamic filtering uses JPA `Specification` (in use by `client` and `tickets`).
- Ticket auditing: `TicketLogManager.generateLogs` diffs the old and new
  ticket field-by-field (`title`, `description`, `solution`, `status`,
  `completedAt`) and groups every change in one update call under a single
  `changeGroupId` (UUID).
- `RoleHierarchy` declares `ADMIN` implies `USER`. `SecurityConfig` permits
  `/worklog/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-ui.html`
  and authenticates everything else. `AuthFilter` runs before
  `UsernamePasswordAuthenticationFilter`.

### Endpoints currently exposed

Confirmed from the controllers in `src/main/java/.../`:

- **auth** (`/worklog/auth`): `POST /register`, `POST /login`,
  `POST /refresh`, `POST /logout`
- **users** (`/users`): `GET /`, `GET /{publicId}`,
  `POST /{publicId}/deactivate` (`@PreAuthorize("hasRole('ADMIN')")`)
- **clients** (`/clients`): `GET ` (with `ClientFiltersParams`),
  `GET /{publicId}`, `POST /`, `PATCH /{publicId}`
- **systems** (`/systems`): `GET `, `GET /{publicId}`, `POST /`,
  `PATCH /{publicId}`
- **tickets** (`/tickets`): `GET ` with `TicketFiltersParams` + `Pageable`
  returning `Page<TicketSummary>`, `POST /create`, `GET /{publicId}`,
  `GET /{publicId}/logs` (paginated `Page<TicketLogResponse>`),
  `PUT /update/{ticketPublicId}` with `@AuthenticationPrincipal User`

## Current Phase

Backend MVP v1 — Phases 1, 2, and 3 of `.claude/mvp-v1-plan.md` shipped
(commits `a3a8b72`, `c812722`, `0b65888`). MVP frontend-blocker phases are
complete. Optional Phases 4–6 are available; awaiting user direction on
whether to continue or stop here.

## Vertical Slices

### Slice A: `GET /users/me` (Phase 3 of MVP v1) — shipped (`0b65888`)
- **API**: add `@GetMapping("/me")` on `UserController` taking
  `@AuthenticationPrincipal User currentUser` and returning `UserResponse`.
- **Service** (optional, for consistency): `UserService.getMe(User)` that
  delegates to `userMapper.toUserResponse`.
- **Data**: no schema changes, no migration; reads only from the principal.
- **Tests (TDD)**: nested class `UserControllerTest$GetMe` — 200 OK with the
  authenticated user's data; populate `SecurityContextHolder` in
  `@BeforeEach` and clear in `@AfterEach`, mirroring the technique used in
  `TicketControllerTest$UpdateTicket`.
- **Verify**: `./mvnw test` green; manual hit with a valid Bearer token.

### Slice B (optional, Phase 4): `POST /users/me/change-password`
- Validate current password, replace hash via `BCryptPasswordEncoder`,
  invalidate the user's refresh tokens in Redis.
- Tests for: wrong current password (422 / 401), success path, eviction of
  prior refresh tokens.

### Slice C (optional, Phase 5): `DELETE /tickets/{publicId}` (soft delete)
- Marks `isEnabled = false`. `GET /tickets` would default to filtering
  `isEnabled = true`.
- Coverage in `TicketSpecification`, `TicketService`, and
  `TicketController` for the new route and the new default filter.

### Slice D (optional, Phase 6): default sort on `GET /tickets`
- When no `?sort=` is supplied, fall back to `updatedAt DESC` so the most
  recently touched tickets appear first. Can live in the controller (default
  `Pageable`) or in `TicketService.findAll` (build `Sort` when the incoming
  `Pageable` is unsorted).

## Deferred / Out of Scope
- Frontend (separate codebase; this repo is backend only).
- Notifications, email, external integrations.
- Rate limiting, observability (metrics, tracing).
- Multi-tenant separation.
- Hard delete and immutable audit (current `TicketLog` rows are JPA-mutable).

## Open Questions
- The auto-memory entry mentions a test-coverage plan at
  `.claude/test-plan.md`, but only `.claude/mvp-v1-plan.md` exists in the
  repository today. Confirm with the user whether the coverage plan was
  consolidated into the MVP plan or simply not committed.
- For Slice B, decide whether changing the password should invalidate the
  user's refresh tokens only, or every active session.
- For Slice C, decide whether soft-deleted tickets should still be visible
  to ADMIN through a query flag (mirroring the `StatusFiltro` enum used in
  the client module).
