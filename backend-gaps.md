# Backend Gaps

Funcionalidades que o frontend precisa mas o backend ainda não expõe.
Atualizar conforme novos gaps forem identificados durante o desenvolvimento.

---

## Tickets

## Sistemas

## Clientes

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
