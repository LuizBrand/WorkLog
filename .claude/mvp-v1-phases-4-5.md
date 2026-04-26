# MVP v1 — Phases 4 & 5

Continuation of `.claude/mvp-v1-plan.md`. Phase 6 was discarded per user
direction. Workflow: **TDD always (tests before implementation), one commit
per phase, pause for approval between phases, full test suite green at the
end of each phase.**

Starting state: commit `b9844d2`, suite at 175/175.

Design decisions confirmed by user:
- Phase 4 — invalidate **only the refresh token tied to the current
  request** (Option B).
- Phase 4 — wrong current password returns **422** (`BusinessException`).
- Phase 5 — soft-delete is **ADMIN-only**.
- Phase 5 — ADMIN can see soft-deleted tickets via a query flag mirroring
  the `StatusFiltro` enum used in the `client` module
  (`ATIVO` / `INATIVO` / `TODOS`).
- Phase 5 — the new `StatusFiltro` field on `TicketFiltersParams` is
  named **`visibility`**, not `status`, because `TicketFiltersParams`
  already has a `TicketStatus status` workflow filter and the two
  concepts collide. `?visibility=ATIVO|INATIVO|TODOS` is the wire name.

---

## Phase 4 — `POST /users/me/change-password`

### Why
Self-service password change unlocks normal account hygiene. Without it the
user has to ask an ADMIN or hit Redis directly.

### Scope
- New DTO `ChangePasswordRequest` (record) in `user/dto/`:
  - `String currentPassword` (`@NotBlank`)
  - `String newPassword` (`@NotBlank`, `@Size(min = 8)`)
  - `String refreshToken` (`@NotBlank`) — required so we know which
    Redis-side session to evict (Option B).
- `UserService.changeMyPassword(User currentUser, ChangePasswordRequest req)`:
  1. `passwordEncoder.matches(req.currentPassword(), currentUser.getPassword())`
     — if false, throw `BusinessException("Senha atual incorreta")` → 422.
  2. Look up the refresh token by id. If absent, or its `userEmail`
     differs from `currentUser.getEmail()`, throw
     `BusinessException("Refresh token inválido para o usuário")` → 422.
     (Prevents using a victim's password change request to wipe a
     stranger's session.)
  3. `currentUser.setPassword(passwordEncoder.encode(req.newPassword()))`
     and `userRepository.save(currentUser)`.
  4. `refreshTokenService.deleteByToken(req.refreshToken())`.
  - Wire `BCryptPasswordEncoder` (already exposed as a bean by
    `SecurityConfig`) and `RefreshTokenService` into `UserService` via
    constructor injection.
- `UserController#changeMyPassword`:
  - `@PostMapping("/me/change-password")`,
    `@AuthenticationPrincipal User currentUser`,
    `@RequestBody @Valid ChangePasswordRequest`.
  - Returns `204 No Content`.
- `UserControllerDocs#changeMyPassword`: 204 / 422 / 400 / 401 ApiResponses.
- No new exception subclass — `BusinessException` is enough for both error
  modes (matches how the rest of the auth flow already returns 422 for
  business-rule failures).
- No schema changes, no migration.

### Tests (TDD order)
1. `UserServiceTest` — new nested class `ChangeMyPassword`:
   - happy path: encoder matches, password is re-hashed, user saved,
     refresh token deleted.
   - wrong current password → `BusinessException` (no save, no delete).
   - refresh token not found → `BusinessException`.
   - refresh token belongs to a different user → `BusinessException`.
2. `UserControllerTest` — new nested class `ChangeMyPassword`:
   - 204 on success (mock service to do nothing).
   - 422 when service throws `BusinessException`.
   - 400 when body is missing fields (Jakarta validation).
   - principal-passthrough: verify the authenticated `User` is passed to
     the service (mirrors `TicketControllerTest$UpdateTicket`).

### Verify
- `./mvnw test` green for the whole suite.
- Manual hit with a real Bearer token — happy path + wrong current
  password (422) + missing refresh token (400).

### Commit
`feat(users): add POST /users/me/change-password`

---

## Phase 5 — `DELETE /tickets/{publicId}` (soft delete) + visibility flag

### Why
Tickets get created in error or for the wrong client. Without a delete the
log just keeps stale entries forever. Soft delete preserves the audit
history; ADMIN can still query the deleted rows when needed.

### Scope
- `TicketService.softDeleteTicket(UUID publicId)`:
  - find by publicId (404 if absent),
  - `ticket.setIsEnabled(false)`,
  - `ticketRepository.save(ticket)`.
- `TicketController#deleteTicket`:
  - `@DeleteMapping("/{publicId}")`,
    `@PreAuthorize("hasRole('ADMIN')")`,
  - returns `204 No Content`.
- `TicketControllerDocs#deleteTicket`: 204 / 404 / 401 / 403.
- Extend `TicketFiltersParams` with `StatusFiltro visibility` (reuse the
  enum already in `client/enums/StatusFiltro.java`). Named `visibility`
  to avoid colliding with the existing `TicketStatus status` workflow
  filter.
- Extend `TicketSpecification` to honor the visibility filter:
  - `ATIVO` → `isEnabled = true`,
  - `INATIVO` → `isEnabled = false`,
  - `TODOS` or `null` → no `isEnabled` predicate (the service decides the
    default upstream).
- `TicketController#findAllTickets` gains
  `@AuthenticationPrincipal User currentUser`, passed through to
  `TicketService.findAll(filters, pageable, currentUser)`.
- `TicketService.findAll` enforces the role rule:
  - if `currentUser` is **not** ADMIN, override `filters.visibility()` to
    `ATIVO` regardless of what was sent (so non-admins can never list
    soft-deleted tickets, even by passing `TODOS` or `INATIVO`).
  - if ADMIN and `filters.visibility()` is `null`, default to `ATIVO` so
    the pre-existing list endpoint behavior does not change for the
    common case.
- No schema changes, no migration (`isEnabled` is on `BaseEntity`).

### Tests (TDD order)
1. `TicketSpecificationTest` — new cases:
   - `visibility = ATIVO` adds the `isEnabled = true` predicate.
   - `visibility = INATIVO` adds the `isEnabled = false` predicate.
   - `visibility = TODOS` adds no `isEnabled` predicate.
   - `visibility = null` adds no `isEnabled` predicate (already covered
     by the all-null baseline; keep that test honest).
2. `TicketServiceTest` — extend `FindAllTickets`:
   - non-ADMIN principal forces the spec to receive `ATIVO` even when the
     caller passed `TODOS` / `INATIVO` / `null`.
   - ADMIN principal preserves the caller's `visibility`, except `null`
     becomes `ATIVO` (default).
3. `TicketServiceTest` — new nested class `SoftDeleteTicket`:
   - happy path: sets `isEnabled = false` and persists.
   - 404 when the ticket does not exist.
4. `TicketControllerTest` — new nested class `DeleteTicket`:
   - 204 on success.
   - 404 when the service throws `TicketNotFoundException`.
   - (no 403 test — `@PreAuthorize` is only enforced when filters are on,
     and `@WebMvcTest` here runs with `addFilters = false`; matches the
     existing `deactiveUserByPublicId` test's stance).
5. `TicketControllerTest$findAllTickets` — extend:
   - the authenticated principal is passed through to the service.
   - the `visibility` query param round-trips into `TicketFiltersParams`.

### Verify
- `./mvnw test` green.
- Manual hit: ADMIN soft-deletes a ticket → `GET /tickets` (default) hides
  it; `GET /tickets?visibility=TODOS` shows it; non-ADMIN gets only
  active even with `?visibility=TODOS`.

### Commit
`feat(tickets): add soft-delete and admin-only deleted visibility`

---

## Definition of done per phase
- [ ] Tests written first, failing for the right reason.
- [ ] Minimal implementation to turn them green.
- [ ] `./mvnw test` green for the whole suite (no regressions).
- [ ] Conventional commit, no Co-Authored-By trailer.
- [ ] Pause for user approval before starting the next phase.
