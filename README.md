# WorkLog

A centralized backend application for support teams to log, track, and manage all customer interactions through a ticket-based system.

## Tech Stack

- **Java 17** + **Spring Boot 3.5.9**
- **PostgreSQL 17** (primary database)
- **Redis 7.4** (refresh token storage with TTL)
- **Flyway** (database migrations)
- **MapStruct** (object mapping)
- **Auth0 java-jwt** (JWT authentication)
- **Lombok**
- **Docker Compose** (PostgreSQL + Redis)

## Architecture

Layered architecture following the **Controller → Service → Repository** pattern, organized by domain:

```
src/main/java/br/com/luizbrand/worklog/
├── auth/              # Authentication (JWT, refresh tokens, filters)
├── client/            # Client management
├── config/            # Security config, data initializer
├── exception/         # Business and resource exceptions
├── exceptionhandler/  # Global REST exception handler
├── role/              # User roles (ADMIN, USER)
├── shared/            # BaseEntity (id, publicId, timestamps, isEnabled)
├── system/            # System/product management
├── tickets/           # Ticket CRUD + audit logging (TicketLog)
└── user/              # User management
```

### Key Design Decisions

- **UUID public identifiers** — entities expose `publicId` (UUID) externally, keeping internal `id` (BIGINT) private
- **Soft deletes** — `isEnabled` flag on `BaseEntity` instead of hard deletion
- **Audit trail** — `TicketLog` tracks all ticket field changes with a `changeGroupId` (UUID) to group related changes from a single update
- **Stateless auth** — JWT access tokens (15 min) + Redis-backed refresh tokens (7 days)
- **Role hierarchy** — ADMIN implies USER via Spring Security `RoleHierarchy`
- **JPA Specifications** — dynamic filtering for clients (by name, status, associated systems)

## API Endpoints

### Authentication (`/worklog/auth`) — Public

| Method | Path        | Description                            |
|--------|-------------|----------------------------------------|
| POST   | `/register` | Register new user (default role: USER) |
| POST   | `/login`    | Login, returns access + refresh token  |
| POST   | `/refresh`  | Refresh access token                   |
| POST   | `/logout`   | Invalidate refresh token               |

### Users (`/users`) — Authenticated

| Method | Path                     | Description                  |
|--------|--------------------------|------------------------------|
| GET    | `/`                      | List all users               |
| GET    | `/{publicId}`            | Get user by ID               |
| POST   | `/{publicId}/deactivate` | Deactivate user (ADMIN only) |

### Clients (`/clients`) — Authenticated

| Method | Path          | Description                                            |
|--------|---------------|--------------------------------------------------------|
| GET    | `/`           | List clients (supports filters: name, status, systems) |
| GET    | `/{publicId}` | Get client by ID                                       |
| POST   | `/`           | Create client                                          |
| PATCH  | `/{publicId}` | Update client                                          |

### Systems (`/systems`) — Authenticated

| Method | Path          | Description      |
|--------|---------------|------------------|
| GET    | `/`           | List all systems |
| GET    | `/{publicId}` | Get system by ID |
| POST   | `/`           | Create system    |
| PATCH  | `/{publicId}` | Update system    |

### Tickets (`/tickets`) — Authenticated

| Method | Path                       | Description                        |
|--------|----------------------------|------------------------------------|
| POST   | `/create`                  | Create ticket                      |
| GET    | `/{publicId}`              | Get ticket by ID                   |
| PUT    | `/update/{ticketPublicId}` | Update ticket (with audit logging) |

## Data Model

### Entities

- **User** — name, email, password; implements `UserDetails`; ManyToMany with Role
- **Role** — name (`ADMIN`, `USER`); implements `GrantedAuthority`
- **Client** — name (unique); ManyToMany with Systems
- **Systems** — name (unique); ManyToMany with Client
- **Ticket** — title, description, solution, status, completedAt; ManyToOne to Client, Systems, User
- **TicketLog** — fieldChanged, fieldType, oldValue, newValue, changeGroupId, changeDate; ManyToOne to Ticket, User, Client, Systems
- **RefreshToken** — Redis Hash with id, userEmail (indexed), TTL-based expiration

### Ticket Statuses

`PENDING` | `AWAITING_CUSTOMER` | `AWAITING_DEVELOPMENT` | `COMPLETED`

### Enums

- `RoleName` — ADMIN, USER
- `TicketStatus` — PENDING, AWAITING_CUSTOMER, AWAITING_DEVELOPMENT, COMPLETED
- `FieldType` — STRING, INTEGER, DECIMAL, BOOLEAN, DATETIME, ENTITY_REF
- `StatusFiltro` — ATIVO, INATIVO, TODOS (client filter)

## Error Handling

Global `RestExceptionHandler` maps exceptions to standardized `ApiExceptionResponse`:

| Exception                         | HTTP Status             |
|-----------------------------------|-------------------------|
| `ResourceNotFoundException`       | 404                     |
| `ResourceAlreadyExistsException`  | 409                     |
| `MethodArgumentNotValidException` | 400 (with field errors) |
| `BusinessException`               | 422                     |
| `RefreshTokenException`           | 401                     |

## Getting Started

### Prerequisites

- Java 17+
- Docker & Docker Compose

### 1. Start infrastructure

```bash
cd Docker
docker compose up -d
```

This starts PostgreSQL (port 5432) and Redis (port 6379). Credentials are configured in the `.env` file.

### 2. Run the application

```bash
./mvnw spring-boot:run
```

The app runs with the `dev` profile by default. Flyway applies migrations automatically on startup. A `DataInitializer` creates default roles (ADMIN, USER) if they don't exist.

### Configuration

| Profile | Database                                                 | SQL Logging |
|---------|----------------------------------------------------------|-------------|
| `dev`   | localhost PostgreSQL                                     | enabled     |
| `prod`  | env vars (`DB_PROD_URL`, `DB_PROD_USER`, `DB_PROD_PASS`) | disabled    |
| `test`  | H2 in-memory                                             | —           |

JWT settings (secret, expiration times) and Redis password are configured via environment variables / `.env` file.

## Database Migrations

Flyway migrations are in `src/main/resources/db/migration/`. Key migrations:

- **V1** — Initial schema: users, roles, clients, systems, tickets, join tables
- **V7** — Ticket audit logging (ticket_logs)
- **V8** — Table rename: work_log_history → ticket_logs
- **V10** — Added client_id, system_id to ticket_logs
