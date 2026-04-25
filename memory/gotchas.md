# Mistakes Log

After any correction from the human, log the pattern here. Review at session
start before starting new work. The goal is to drive the error rate toward zero.

Format:
- **Date** — *mistake* → **rule**

---

- **2026-04-25** — *Injecting `PasswordEncoder` into `UserService` triggered a `BeanCurrentlyInCreationException` (cycle: `UserService` → `SecurityConfig` → `AuthFilter` → `CustomUserDetailsService` → `UserService`)* → **Any service that already participates in `CustomUserDetailsService`'s graph must declare `PasswordEncoder` (and other beans defined inside `SecurityConfig`) with `@Lazy` on the constructor parameter, or pull the bean out of `SecurityConfig`. Run the full suite, not just the focused tests, to catch this — only `WorklogApplicationTests` boots the full context.**
