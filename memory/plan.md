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

**Fase 6 — integração CNPJá** (branch `feat/client-registration-expasion`),
plano em `FASE-6-integracao-cnpja.md`. Fatia **6A entregue** em 2026-07-28
(`GET /clients/lookup`); faltam 6B (inscrição estadual, regime tributário,
situação cadastral) e 6C (cache no Redis).

Antes disso: o backend MVP v1 (fases 1–5) e a **expansão do cadastro de
clientes** (empresa + filiais + documento + contatos + endereço, fases 1–5 do
`PLANO-expansao-cadastro-clientes.md`) foram entregues e verificados ponta a
ponta contra o Postgres real. O detalhamento por fatia, com os porquês, está em
`memory/progress.md` — a descrição de módulos e endpoints abaixo é anterior à
expansão e **não** lista as rotas de filial.

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

### Slice C (Phase 5): `DELETE /tickets/{publicId}` (soft delete)
- Marks `isEnabled = false` (`@PreAuthorize("hasRole('ADMIN')")`).
- New `StatusFiltro visibility` field on `TicketFiltersParams` (named
  `visibility` rather than `status` to avoid colliding with the existing
  `TicketStatus status` workflow filter). `ATIVO` / `INATIVO` / `TODOS`.
- Service forces `visibility = ATIVO` for non-ADMIN principals; for
  ADMIN, `null` defaults to `ATIVO`.
- Coverage in `TicketSpecification`, `TicketService`, and
  `TicketController` for the new route and the new default filter.

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
