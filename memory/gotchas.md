# Mistakes Log

After any correction from the human, log the pattern here. Review at session
start before starting new work. The goal is to drive the error rate toward zero.

Format:
- **Date** — *mistake* → **rule**

---

- **2026-04-25** — *Injecting `PasswordEncoder` into `UserService` triggered a `BeanCurrentlyInCreationException` (cycle: `UserService` → `SecurityConfig` → `AuthFilter` → `CustomUserDetailsService` → `UserService`)* → **Any service that already participates in `CustomUserDetailsService`'s graph must declare `PasswordEncoder` (and other beans defined inside `SecurityConfig`) with `@Lazy` on the constructor parameter, or pull the bean out of `SecurityConfig`. Run the full suite, not just the focused tests, to catch this — only `WorklogApplicationTests` boots the full context.**

- **2026-05-10** — *Tried `./mvnw spring-boot:run` directly; failed with `Failed to bind properties under 'security.jwt.expiration' to long: Value: "${JWT_EXPIRATION}"` because `application-dev.yaml` uses `${VAR}` placeholders and Spring does not auto-load the project `.env`* → **To boot the app from the CLI in dev, source `.env` into the shell first: `set -a; . ./.env; set +a; ./mvnw spring-boot:run`. The agent cannot `cat` the `.env` (credential file is blocked from reads), but `set -a; .` exports values without exposing them in the log. The user normally launches via IntelliJ which has env vars configured per run-config — that path bypasses this entirely.**

- **2026-05-10** — *After `docker compose up -d` the app failed Flyway with `FATAL: password authentication failed for user "worklog"` even though `.env` looked correct.* → **The `pgdata` Docker volume persists the credentials used at the very first `up`. Editing `.env` afterwards does not propagate. To reset: `cd Docker && docker compose down -v && docker compose up -d` (destroys dev data — confirm with the user before running). Less destructive: `docker exec -it worklog-db psql -U <current-user> -c "ALTER USER worklog WITH PASSWORD '...';"` if you know the current creds.**

- **2026-05-10** — *I claimed in memory that `application-test.yaml` (H2 + Flyway disabled) reflected the actual test config, but the user noted the test DB is Postgres.* → **The YAML on disk says H2, but the user's working assumption is Postgres-for-tests. Treat the YAML as potentially stale until clarified or updated. If working on test-config changes, ask before assuming the YAML is the source of truth.**

- **2026-05-10** — *In `ClientServiceTest`, the inner mapper test class was declared `static class ClientMapperTest` with `@ExtendWith(MockitoExtension.class)` — looked runnable but JUnit 5 silently skipped all 5 tests inside it. I noticed only because my Phase 5 mapper tests didn't increment the suite count.* → **JUnit 5 only auto-discovers nested test containers that are non-static `@Nested` inner classes. A `static` inner class with `@Test` methods is dead code unless explicitly named in a runner. Whenever a test count looks lower than expected, check for `static class` inside a test class and convert it to `@Nested` (drop redundant inherited annotations like `@ActiveProfiles`/`@ExtendWith` from the outer class).**

- **2026-07-15** — *Rodei `docker-compose.prod.yaml` (em `Docker/`) sem project name próprio; como o compose de dev vive no mesmo diretório e também define serviços `postgres`/`redis`, o Compose tratou-os como os mesmos serviços do projeto default `docker` e recriou/renomeou os containers de dev (`worklog-db`→`worklog-db-prod`).* → **Dois compose files no mesmo diretório compartilham o project name default (basename do dir). Serviços homônimos colidem. Sempre isolar a stack de prod com `name: <projeto>` no próprio arquivo (feito: `name: worklog-prod`) e evitar `container_name` fixo (impede coexistência entre projetos). Para limpar leftovers de um projeto sem apagar volumes, usar `docker compose -f <file> -p <projeto> down` (sem `-v`); `docker rm -f` costuma ser bloqueado pela permissão do harness.**
