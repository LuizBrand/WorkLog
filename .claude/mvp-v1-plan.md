# MVP v1 — endpoints mínimos para o frontend

Plano para fechar as lacunas do backend antes de começar o frontend da v1.
Workflow: **TDD sempre (testes antes da implementação), um commit por fase, pausa para aprovação entre fases, suite verde ao fim de cada fase**.

Estado inicial: commit `07e410b`, 150/150 testes verdes.

---

## Fase 1 — `GET /tickets` com filtros e paginação **(bloqueador)**

**Por quê:** hoje o log compartilhado é invisível. Um ticket só é recuperável pelo UUID. O frontend precisa listar.

**Escopo:**
- Novo record `TicketFiltersParams` em `tickets/dto/`:
  - `String title` (LIKE case-insensitive)
  - `TicketStatus status`
  - `UUID clientId`
  - `UUID systemId`
  - `UUID userId`
  - `LocalDate createdFrom` / `LocalDate createdTo` (range em `createdAt`)
- Nova classe `TicketSpecification` seguindo o padrão de `ClientSpecification`.
- `TicketRepository` passa a estender `JpaSpecificationExecutor<Ticket>`.
- `TicketService.findAll(TicketFiltersParams, Pageable)` → `Page<TicketResponse>`.
- `GET /tickets` no `TicketController` com `TicketFiltersParams` + `Pageable` (Spring Data resolve `?page=&size=&sort=`).
- Novo DTO `TicketSummary` (leve, sem `solution`, sem logs) para a resposta da lista — retorno `Page<TicketSummary>`; a busca individual continua retornando `TicketResponse` completo.
- Atualizar `TicketMapper` com `toSummary`.

**Testes (ordem TDD):**
1. `TicketSpecificationTest` — pure-Mockito no Criteria API (padrão do `ClientSpecificationTest`): todos filtros nulos, cada filtro isolado, `title` vazio ignorado, range de datas (só `from`, só `to`, ambos).
2. `TicketMapperTest` — adicionar casos de `toSummary` (usando `Mappers.getMapper`).
3. `TicketServiceTest` — nova classe aninhada `FindAllTickets`: delega para repo com spec + pageable, mapeia cada item, retorna `Page` vazio quando repo retorna vazio.
4. `TicketControllerTest` — nova classe aninhada `findAllTickets`: 200 OK com passagem de filtros e parâmetros de paginação, resposta vem como `Page`.

**Commit:** `feat(tickets): add list endpoint with filters and pagination`

---

## Fase 2 — Logs de auditoria visíveis no frontend

**Por quê:** `TicketLog` já é gerado em toda atualização, mas não há endpoint que exponha. Auditoria sem visibilidade é inútil para o usuário final.

**Decisão de design:** endpoint separado `GET /tickets/{publicId}/logs` (não embutir na `TicketResponse`). Motivos: logs podem crescer muito; paginar separadamente; lista de tickets não carrega logs.

**Escopo:**
- Novo DTO `TicketLogResponse` em `tickets/dto/`:
  - `UUID changeGroupId`, `String fieldChanged`, `FieldType fieldType`, `String oldValue`, `String newValue`, `LocalDateTime changeDate`, `UserSummary user`.
- **Corrigir typo `newValeu` → `newValue`** em `TicketLog.java` (com migration Flyway renomeando a coluna — já é MVP, melhor limpar agora).
- `TicketLogRepository.findByTicketPublicIdOrderByChangeDateDesc(UUID, Pageable)` (ou via spec).
- Novo método `TicketLogManager.findLogsByTicket(UUID ticketPublicId, Pageable)` → `Page<TicketLogResponse>`.
- `TicketMapper.toLogResponse(TicketLog)`.
- Novo endpoint no `TicketController`: `GET /tickets/{publicId}/logs` → 200 OK com `Page<TicketLogResponse>` (ou 404 se o ticket não existir).

**Testes (TDD):**
1. `TicketLogManagerTest` — adicionar casos de `findLogsByTicket`: 404 quando ticket não existe, retorno paginado com mapeamento.
2. `TicketMapperTest` — casos de `toLogResponse`.
3. `TicketControllerTest` — nova classe aninhada `getTicketLogs`: 200 OK + 404.

**Commit:** `feat(tickets): expose audit log via GET /tickets/{id}/logs`

---

## Fase 3 — `GET /users/me`

**Por quê:** o frontend precisa de um endpoint padrão para saber quem está logado (header, avatar, permissões). Fazer o frontend bater em `GET /users/{id}` com o id do JWT é feio e exige o frontend parsear o token.

**Escopo:**
- Novo endpoint `GET /users/me` no `UserController`:
  - Recebe `@AuthenticationPrincipal User currentUser`.
  - Retorna `UserResponse` (já existe).
- Não precisa de service novo — só mapear o `currentUser` e retornar. Se quiser consistência com o resto do código, criar `userService.getMe(User)` que apenas chama o mapper.

**Testes (TDD):**
1. `UserControllerTest` — nova classe aninhada `getMe`:
   - 200 OK com os dados do usuário autenticado (setar `SecurityContextHolder` como no `TicketControllerTest$UpdateTicket`).
   - 401 já é coberto pelo filtro quando não há auth — não precisamos testar isso aqui.

**Commit:** `feat(users): add GET /users/me endpoint`

---

## Fases opcionais (apenas se quisermos escopo maior antes da v1)

### Fase 4 (opcional) — `POST /users/me/change-password`
- Valida senha atual, atualiza hash, invalida refresh tokens do usuário no Redis (segurança básica).

### Fase 5 (opcional) — `DELETE /tickets/{publicId}` (soft delete)
- Marca `isEnabled = false`. Lista passa a filtrar só `isEnabled = true` por padrão.

### Fase 6 (opcional) — ordenação por `updatedAt DESC` como default em `GET /tickets`
- Usuário vê os tickets mais recentemente mexidos primeiro.

---

## Definição de pronto para cada fase
- [ ] Testes escritos antes da implementação, falhando pelo motivo certo.
- [ ] Implementação mínima para passar os testes.
- [ ] `./mvnw test` verde (nenhuma regressão nos 150 já existentes).
- [ ] Commit com mensagem conventional, sem trailer de Claude.
- [ ] Pausa para aprovação do usuário antes da próxima fase.

## Ao final das Fases 1–3
Backend pronto o suficiente para começar o frontend da v1:
- Listar e filtrar o log de atendimentos.
- Criar, ver detalhes e atualizar um ticket.
- Ver histórico de alterações de cada ticket.
- Autenticar, saber quem está logado, manter sessão via refresh token.
- CRUD de clientes, sistemas (gestão); desativar usuário (admin).
