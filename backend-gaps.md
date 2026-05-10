# Backend Gaps

Funcionalidades que o frontend precisa mas o backend ainda não expõe.
Atualizar conforme novos gaps forem identificados durante o desenvolvimento.

---

## Tickets

### 2. Status `CANCELLED`

**Onde falta:** `TicketRequest.status` / `TicketUpdateRequest.status` — enum não inclui `CANCELLED`  
**O que falta:** suporte ao estado "Cancelado" no backend (hoje só existem `PENDING`, `AWAITING_CUSTOMER`, `AWAITING_DEVELOPMENT`, `COMPLETED`)  
**Impacto:** botão "Cancelado" existe na UI mas está desabilitado; status é UI-only até o backend suportar  
**Frontend:** `UI_STATUS_WRITABLE` já exclui `CANCELLED`; botão desabilitado com `cursor-not-allowed`

---

### 3. Campo `priority` ausente em `TicketSummary`

**Onde falta:** `GET /tickets` — schema `TicketSummary`  
**O que falta:** campo `priority` (CRITICAL / HIGH / MEDIUM / LOW) na listagem de tickets  
**Impacto:** coluna PRIORIDADE na tabela sempre exibe "—"  
**Frontend:** coluna existe na tabela, aguardando campo no response

---

## Sistemas

### 4. `SystemResponse` não expõe campo de status ativo/inativo

**Onde falta:** `GET /systems` — schema `SystemResponse`  
**O que falta:** campo `enabled` (ou equivalente) para indicar se o sistema está ativo  
**Impacto:** no formulário de criação de ticket, sistemas inativos aparecem na lista de seleção junto com os ativos; não há como filtrar client-side sem esse campo  
**Frontend:** `GET /clients` já suporta `?filtersParams.status=ATIVO` e foi aplicado no create dialog; aguardando equivalente para sistemas

---

## Clientes

### 5. `ClientRequest` não expõe campo `enabled` (sem endpoint de desativar/reativar)

**Onde falta:** `PATCH /clients/{publicId}` — `ClientRequest`
**O que falta:** campo `enabled` para desativar ou reativar um cliente via update
**Impacto:** não há como alterar o status ATIVO/INATIVO de um cliente pela UI; botão "Desativar" omitido do painel de detalhe
**Frontend:** `ClientStatusBadge` exibe o status atual mas sem ação associada

---

### 6. Sem endpoint `DELETE /clients/{publicId}`

**Onde falta:** `clientes.ts` — nenhuma operação DELETE gerada pelo Orval
**O que falta:** endpoint para excluir clientes
**Impacto:** menu de ações da tabela de clientes não tem opção "Excluir"; apenas Ver detalhes e Editar disponíveis
**Frontend:** `ClientTable` omite ação de exclusão por falta de suporte no backend

---

## Autenticação

### 7. Tokens via HttpOnly cookies em vez de corpo JSON

**Onde falta:** `POST /auth/login` e `POST /auth/refresh` — response body  
**O que falta:** emitir `accessToken` e `refreshToken` em cookies HttpOnly/Secure/SameSite=Strict em vez de retorná-los no JSON; o endpoint de refresh deve ler o cookie automaticamente (sem corpo de request)  
**Impacto:** tokens atualmente armazenados em `localStorage` são acessíveis a qualquer JavaScript na página (vulnerabilidade XSS); cookies HttpOnly são opacos ao JS e eliminam essa superfície de ataque  
**Frontend:** `src/state/auth.ts` usa `localStorage` via Zustand `persist`; `src/lib/api.ts` injeta o token via header `Authorization: Bearer`; ambos precisarão ser adaptados quando o backend suportar cookies (remover persist de tokens, remover interceptor de injeção manual, adicionar `withCredentials: true` nas chamadas Axios)

---

## A adicionar conforme desenvolvimento avança

<!-- Exemplo:
### N. <nome da feature>
**Onde falta:** `<MÉTODO> /endpoint`
**O que falta:** descrição
**Impacto:** impacto no frontend
**Frontend:** estado atual da UI
-->
