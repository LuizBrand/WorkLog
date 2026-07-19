# Criação de usuários (somente ADMIN) — guia para o Frontend

Este documento descreve como funciona a criação de usuários no backend do WorkLog,
para o frontend adaptar a tela de usuários e liberar o cadastro **apenas para usuários ADMIN**.

## Resumo (TL;DR)

- **Endpoint:** `POST /worklog/auth/register`
- **Quem pode chamar:** apenas usuários autenticados com a role **ADMIN**.
- **O que cria:** um novo usuário com a role fixa **USER** (não é possível criar outro ADMIN por este endpoint).
- **Autenticação:** por **cookies HttpOnly** (não há token no header/localStorage — ver seção "Autenticação").
- **Sucesso:** `201 Created` com os dados do usuário criado.

> ✅ Está tudo configurado corretamente no backend. A restrição a ADMIN é garantida
> na camada de segurança (`SecurityConfig`), independente do que o frontend enviar.
> O frontend deve apenas espelhar essa regra na UI (esconder/mostrar o botão de criar).

---

## Autenticação (como as requisições são autorizadas)

O WorkLog **não usa Bearer token no header**. A sessão é mantida por **cookies HttpOnly**
emitidos no login:

- `worklog_access` — access token (path `/`)
- `worklog_refresh` — refresh token (path `/worklog/auth`)

Ambos são `HttpOnly`, `SameSite=Strict` (e `Secure` em produção). Isso significa:

1. O JavaScript **não consegue ler** esses cookies (é proposital, protege contra XSS).
2. Toda requisição ao backend precisa enviar os cookies automaticamente. No `fetch`/axios use:
   - `fetch(url, { credentials: "include" })`
   - axios: `withCredentials: true`
3. Se o access token expirar, chame `POST /worklog/auth/refresh` para renovar antes de repetir a chamada.

### Como o frontend sabe se o usuário logado é ADMIN?

Como o token não pode ser lido no JS, use o endpoint de perfil:

- `GET /users/me` → retorna o usuário logado, incluindo a lista de `roles`.
- Se `roles` contiver `{ "role": "ADMIN" }`, exiba os controles de criação de usuário.

Exemplo de resposta de `GET /users/me`:

```json
{
  "publicId": "3f2b1c9a-....",
  "email": "admin@empresa.com",
  "name": "Administrador",
  "roles": [ { "role": "ADMIN" } ],
  "createdAt": "2026-07-19T10:00:00"
}
```

> A UI deve tratar isso como conveniência visual. Mesmo que um usuário não-admin
> force a chamada, o backend responde `403 Forbidden`.

---

## Endpoint de criação de usuário

### Request

```
POST /worklog/auth/register
Content-Type: application/json
Cookie: worklog_access=...   (enviado automaticamente com credentials: "include")
```

#### Body

```json
{
  "name": "João da Silva",
  "email": "joao@empresa.com",
  "password": "Senha123"
}
```

#### Regras de validação dos campos

| Campo      | Obrigatório | Regras |
|------------|-------------|--------|
| `name`     | Sim         | Entre **2 e 100** caracteres, não pode ser vazio |
| `email`    | Sim         | Formato de email válido, **único** no sistema |
| `password` | Sim         | Mínimo **8** caracteres, deve conter **pelo menos 1 letra maiúscula, 1 minúscula e 1 número** |

> Recomenda-se replicar essas validações no formulário do frontend para dar feedback
> imediato, mas o backend é a fonte de verdade e valida novamente.

### Resposta de sucesso — `201 Created`

```json
{
  "publicId": "8c1f0e42-....",
  "name": "João da Silva",
  "email": "joao@empresa.com",
  "createdAt": "2026-07-19T14:32:10"
}
```

> A senha nunca é retornada. O novo usuário recebe automaticamente a role **USER**.

### Respostas de erro

| Status | Quando acontece | Ação sugerida na UI |
|--------|-----------------|---------------------|
| `400 Bad Request` | Campos inválidos (name/email/password fora das regras) | Mostrar mensagens de validação por campo |
| `401 Unauthorized` | Usuário não autenticado (sem cookie válido) | Redirecionar para login / tentar `refresh` |
| `403 Forbidden` | Autenticado, mas **não é ADMIN** | Mostrar "sem permissão"; esconder o botão de criar |
| `409 Conflict` | Email já cadastrado | Mostrar "Este email já está em uso" |

Formato do corpo de erro (400/401/409), padrão `ApiExceptionResponse`:

```json
{
  "timestamp": "2026-07-19T14:32:10",
  "status": 409,
  "error": "...",
  "message": "Email 'joao@empresa.com' já está em uso.",
  "path": "/worklog/auth/register"
}
```

---

## Exemplo de chamada (fetch)

```js
async function createUser({ name, email, password }) {
  const res = await fetch("https://SEU_BACKEND/worklog/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include", // ESSENCIAL: envia os cookies HttpOnly
    body: JSON.stringify({ name, email, password }),
  });

  if (res.status === 201) return res.json();       // usuário criado
  if (res.status === 403) throw new Error("Sem permissão (requer ADMIN).");
  if (res.status === 409) throw new Error("Email já cadastrado.");
  if (res.status === 400) throw new Error("Dados inválidos.");
  if (res.status === 401) throw new Error("Sessão expirada.");
  throw new Error("Erro inesperado.");
}
```

---

## Endpoints relacionados (tela de usuários)

Todos exigem autenticação. Base sem prefixo de contexto (ex.: `http://host:8080`).

| Método | Endpoint | Permissão | Descrição |
|--------|----------|-----------|-----------|
| `POST` | `/worklog/auth/register` | **ADMIN** | Cria novo usuário (role USER) |
| `GET`  | `/users/` | Autenticado | Lista todos os usuários |
| `GET`  | `/users/me` | Autenticado | Dados do usuário logado (use para checar se é ADMIN) |
| `GET`  | `/users/{publicId}` | Autenticado | Detalhe de um usuário |
| `POST` | `/users/{publicId}/deactivate` | **ADMIN** | Desativa (soft delete) um usuário |
| `POST` | `/users/me/change-password` | Autenticado | Troca a própria senha |

> Observação: o identificador usado em todas as rotas e respostas é o `publicId` (UUID),
> nunca o id interno.

---

---

## Troca de senha (usuário logado)

Qualquer usuário autenticado pode trocar a **própria** senha. Não existe endpoint para
um usuário trocar a senha de outro.

### Request

```
POST /users/me/change-password
Content-Type: application/json
Cookie: worklog_access=...   (enviado automaticamente com credentials: "include")
```

#### Body

```json
{
  "currentPassword": "SenhaAtual123",
  "newPassword": "SenhaNova123"
}
```

> Não é necessário (nem possível) enviar o refresh token — ele fica em cookie HttpOnly.
> O backend identifica o usuário pela sessão autenticada.

#### Regras de validação

| Campo             | Obrigatório | Regras |
|-------------------|-------------|--------|
| `currentPassword` | Sim         | Senha atual do usuário (não pode ser vazia) |
| `newPassword`     | Sim         | Mínimo **8** caracteres, com **1 maiúscula, 1 minúscula e 1 número** |

### Comportamento importante — revoga TODAS as sessões 🔒

Ao trocar a senha com sucesso, o backend **revoga todas as sessões ativas do usuário**
(todos os refresh tokens, em todos os dispositivos). Isso é proposital por segurança:
se a senha foi comprometida, nenhuma sessão antiga continua válida.

**Consequência para o frontend:** após o `204`, a sessão atual também deixa de poder
ser renovada. O frontend **deve encerrar a sessão e redirecionar para a tela de login**,
para o usuário entrar de novo com a nova senha. Recomenda-se chamar `POST /worklog/auth/logout`
(que limpa os cookies) e então mandar para o login.

### Respostas

| Status | Quando acontece | Ação sugerida na UI |
|--------|-----------------|---------------------|
| `204 No Content` | Senha alterada com sucesso | Deslogar (chamar `/worklog/auth/logout`) e redirecionar para login com aviso "Senha alterada, entre novamente" |
| `400 Bad Request` | `newPassword` fora das regras / campos ausentes | Mostrar validação por campo |
| `401 Unauthorized` | Não autenticado | Redirecionar para login |
| `422 Unprocessable Entity` | **Senha atual incorreta** | Mostrar erro no campo "senha atual" |

### Exemplo (fetch)

```js
async function changeMyPassword({ currentPassword, newPassword }) {
  const res = await fetch("https://SEU_BACKEND/users/me/change-password", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ currentPassword, newPassword }),
  });

  if (res.status === 204) {
    // Todas as sessões foram revogadas — encerrar e voltar ao login.
    await fetch("https://SEU_BACKEND/worklog/auth/logout", {
      method: "POST",
      credentials: "include",
    });
    // redirecionar para /login com mensagem de sucesso
    return;
  }
  if (res.status === 422) throw new Error("Senha atual incorreta.");
  if (res.status === 400) throw new Error("Nova senha inválida.");
  if (res.status === 401) throw new Error("Sessão expirada.");
  throw new Error("Erro inesperado.");
}
```

---

## Notas para a UI

1. **Só exiba o botão/formulário "Criar usuário" e "Desativar" quando `GET /users/me` indicar role ADMIN.**
2. Sempre usar `credentials: "include"` (ou `withCredentials: true`) nas chamadas — senão o backend responde `401`.
3. Ao receber `401` em qualquer chamada, tentar `POST /worklog/auth/refresh` uma vez e repetir; se falhar, redirecionar para login.
4. O endpoint de criação não permite escolher a role: todo usuário criado por aqui é **USER**. Se no futuro for necessário criar ADMIN pela tela, isso exige mudança no backend (hoje o ADMIN inicial vem de seed por variável de ambiente).
