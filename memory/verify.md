# Definition of Done

Every task's verification criteria must pass before it is marked complete in
`progress.md`. No exceptions.

## Build & Compile (always)
- [ ] `./mvnw clean compile` passes — confirms Lombok and MapStruct generated code correctly.
- [ ] No new compiler warnings introduced by the change.

## Tests (always)
- [ ] `./mvnw test` green for the **entire** suite. Per `CLAUDE.md`: "the full suite must be green before a change is considered done."
- [ ] New tests were written **before** the implementation (TDD), failed for the right reason, then turned green with the minimum implementation.
- [ ] No test was annotated `@Disabled` / `@Ignore` to make the suite pass.
- [ ] Controller tests that depend on `@AuthenticationPrincipal` populate `SecurityContextHolder` in `@BeforeEach` and clear it in `@AfterEach` — required because `@WebMvcTest` runs with `addFilters = false`.
- [ ] Mapper tests use `Mappers.getMapper(X.class)` (no `@SpringBootTest`).
- [ ] Controller tests declare `@MockitoBean AuthFilter` and `@MockitoBean CustomUserDetailsService`.

## Project conventions (always)
- [ ] No `@Autowired`. Constructor injection is hand-written in services and controllers.
- [ ] New entities extend `BaseEntity` and use `@SuperBuilder` if necessary.
- [ ] New DTOs are immutable records. Request records use Jakarta Validation where applicable; PATCH-style requests have nullable fields.
- [ ] New mappers use `componentModel = "spring"` and `NullValuePropertyMappingStrategy.IGNORE` when applying partial updates.
- [ ] Endpoints expose only `publicId` (UUID). Internal `id` (Long) is never returned or accepted.
- [ ] Schema changes go in a new Flyway migration `V{n+1}__description.sql` — never edit a migration that has already been applied, and never use `ddl-auto: create`/`update`.
- [ ] New exceptions extend one of the three base classes (`ResourceNotFoundException`, `ResourceAlreadyExistsException`, `BusinessException`) so `RestExceptionHandler` catches them automatically.
- [ ] Public endpoints stay limited to `/worklog/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-ui.html`. Everything else requires authentication.

## Tactile verification (when code runs)
- [ ] Bring up the dependencies: `cd Docker && docker compose up -d`.
- [ ] `./mvnw spring-boot:run` (default `dev` profile) starts cleanly and Flyway applies all migrations without `validate` failures.
- [ ] New endpoint exercised manually with a valid Bearer token (curl or Swagger UI at `/swagger-ui`) — happy path **plus** at least one edge case (404, 422, 401 as appropriate).
- [ ] Application logs show no unexpected exceptions, Hibernate warnings, or silent stack traces.

## Visual verification (UI changes only)
- N/A — backend-only repository.

## Independent verification
- [ ] At least one of: (a) new tests failed before the change and passed after, (b) human review prior to commit, (c) `/review` or `/security-review` sub-agent review.

## Git & commits
- [ ] No `git push`, `--force`, `reset --hard`, or `--no-verify` without explicit user authorization.
- [ ] Commit messages follow Conventional Commits (e.g. `feat(tickets): add list endpoint with filters and pagination`).
- [ ] **No `Co-Authored-By: Claude` trailer** (user preference recorded in user-level memory).
- [ ] One commit per phase of a multi-step plan. Pause for user approval before starting the next phase.
- [ ] Never commit `.env` or any secret/credential file.

## Task-specific criteria

### Slice A — `GET /users/me` (MVP Phase 3)
- [ ] `UserControllerTest$GetMe` written before the implementation.
- [ ] 200 OK returns `UserResponse` for the authenticated principal.
- [ ] Manual confirmation with a valid Bearer token of an existing user.
- [ ] Commit `feat(users): add GET /users/me endpoint`.
