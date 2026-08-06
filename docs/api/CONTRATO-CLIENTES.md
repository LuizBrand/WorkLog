# Contrato de API — Módulo Clientes (expansão de cadastro)

Documento de integração para o **frontend**. Descreve os endpoints novos e alterados do
módulo de clientes, as regras de validação, os erros e o que cada tela precisa fazer.

Gerado a partir do backend na branch `feat/client-registration-expasion`
(fases 1–6: filiais, contatos, endereço, documentos, consulta de CNPJ).

---

## 0. Como usar este pacote

| Arquivo | O que é | Quando usar |
|---|---|---|
| `CONTRATO-CLIENTES.md` | Este documento. Regras de negócio, estados, erros e o que cada tela faz. | **Autoridade máxima.** Leia antes de codar. |
| `client-api.ts` | Tipos TypeScript escritos à mão para o módulo de clientes, com obrigatoriedade correta. | Importe nos componentes e services do módulo de clientes. |
| `openapi.json` | Spec OpenAPI 3.1 exportada da API rodando (`/v3/api-docs`). Cobre a API inteira. | Consulta de outros módulos (tickets, users, systems) e conferência de shape. |
| `api.d.ts` | Tipos gerados do `openapi.json` via `openapi-typescript`. | Consulta de outros módulos. |

**Ordem de autoridade:** este markdown > `client-api.ts` > `openapi.json` > `api.d.ts`.

> **Por que os tipos gerados não bastam:** o springdoc só marca como `required` os campos
> anotados com `@NotNull`. Campos `@NotBlank` e `@NotEmpty` — `name` e `branches` do
> cliente, `valor` do contato — saem como opcionais no `api.d.ts`, mas o backend
> **rejeita** com 400 se vierem vazios. Use o `client-api.ts` para o módulo de clientes.

---

## 1. Integração base

- **Base URL (dev):** `http://localhost:8080` — sem context-path.
- **Autenticação: cookie `httpOnly`, não header.** O backend lê o JWT do cookie
  `worklog_access`. Mandar `Authorization: Bearer ...` **não autentica**.
  - Todo request precisa de `credentials: 'include'` (fetch) ou
    `withCredentials: true` (axios). Sem isso, tudo volta 401.
  - Nunca guarde token em `localStorage`/`sessionStorage` — não há token acessível ao JS.
  - `POST /worklog/auth/login` seta `worklog_access` + `worklog_refresh`;
    `POST /worklog/auth/refresh` renova (usa o cookie de refresh, sem body);
    `POST /worklog/auth/logout` limpa os dois.
  - Em 401, chame `refresh` uma vez e repita o request original; se falhar de novo,
    mande para o login.
- **CORS:** origem liberada é `http://localhost:3000`. Headers permitidos: apenas
  `Content-Type` e `Accept`. **Header customizado é bloqueado pelo preflight** — não
  invente `X-Request-Id` e afins.
- **Content-Type:** `application/json` em todo POST/PATCH.
- **IDs:** sempre `publicId` (UUID). O id numérico interno nunca é exposto.
- **Datas:** `createdAt` é ISO-8601 com offset UTC (`2026-07-29T13:45:12.123456Z`).

### Formato de erro (único, para todos os status)

```ts
type ApiExceptionResponse = {
  timestamp: string;                        // "2026-07-29T13:45:12.123"
  status: number;                           // 400, 404, 409, 422, 429, 503
  error: string;                            // "Bad Request", "Conflict", ...
  message: string;                          // mensagem exibível (pt-BR na maioria dos casos)
  path: string;                             // "/clients/"
  fieldErrors?: Record<string, string>;     // SOMENTE em 400 de Bean Validation
};
```

**Regra de tratamento:**
- `fieldErrors` presente → erro por campo, renderize embaixo do input correspondente.
  A chave é o caminho do campo, incluindo índice de array: `branches[0].documento`,
  `branches[1].contatos[0].valor`.
- `fieldErrors` ausente → erro global, use toast/alert com `message`.

---

## 2. Enums

Envie e receba exatamente estas strings.

| Enum | Valores | Onde |
|---|---|---|
| `ClientType` | `PJ`, `PF` | `tipo` do cliente |
| `RegimeTributario` | `SIMPLES_NACIONAL`, `LUCRO_PRESUMIDO`, `LUCRO_REAL`, `MEI`, `IMUNE`, `ISENTO` | `regimeTributario` do cliente |
| `ContactType` | `EMAIL`, `TELEFONE`, `CELULAR`, `WHATSAPP` | `tipo` do contato |
| `StatusFiltro` | `ATIVO`, `INATIVO`, `TODOS` | query param `status` |

Labels sugeridos para a UI (o backend não envia label):

```
PJ → "Pessoa Jurídica"          SIMPLES_NACIONAL → "Simples Nacional"
PF → "Pessoa Física"            LUCRO_PRESUMIDO  → "Lucro Presumido"
                                LUCRO_REAL       → "Lucro Real"
EMAIL    → "E-mail"             MEI              → "MEI"
TELEFONE → "Telefone"           IMUNE            → "Imune"
CELULAR  → "Celular"            ISENTO           → "Isento"
WHATSAPP → "WhatsApp"
```

---

## 3. Modelo de domínio (o que mudou conceitualmente)

Antes, um cliente era um registro plano. Agora:

```
Client (tipo, name, nomeFantasia, regimeTributario, systems[])
  └── branches[]  (ClientBranch — "filial" / unidade)
        ├── documento (CPF ou CNPJ, único GLOBALMENTE)
        ├── isMatriz  (exatamente uma por cliente)
        ├── apelido, inscricaoEstadual, inscricaoMunicipal
        ├── address   (embutido, todo opcional)
        └── contatos[] (no máximo um com principal: true)
```

Invariantes que a UI deve respeitar:

1. **Todo cliente tem exatamente uma matriz.** Nasce no `POST /clients`, e depois só
   muda por transferência (`PATCH` de outra filial com `isMatriz: true`).
2. **`documento` é único em toda a base**, não por cliente. Inclusive de filial inativa —
   inativar não libera o documento.
3. **Cliente `PF` tem apenas a matriz.** Nenhuma filial adicional, nunca.
4. **`tipo` do cliente define o documento das filiais:** `PF` → CPF, `PJ` → CNPJ
   (checksum validado, formato alfanumérico de 2026 aceito).
5. **O backend normaliza `documento`** (remove máscara, aplica maiúsculas), **`cep`**
   (só dígitos) e **`uf`** (maiúsculas). Pode enviar com máscara; a resposta vem limpa.
   Se a UI mostra máscara, formate na exibição.

---

## 4. Breaking changes — telas que param de funcionar

### 4.1 `POST /clients` virou `POST /clients/` (com barra final)

O mapeamento é literalmente `/clients/`. `POST /clients` **sem** a barra não resolve.
Corrija no service do front.

### 4.2 O payload de criação de cliente ficou aninhado

**Antes:**
```json
{ "name": "Empresa X", "systemsPublicIds": ["uuid"], "enabled": true }
```

**Agora:**
```json
{
  "tipo": "PJ",
  "name": "Empresa X",
  "nomeFantasia": "X",
  "regimeTributario": "SIMPLES_NACIONAL",
  "systemsPublicIds": ["uuid"],
  "branches": [
    { "documento": "11222333000181", "isMatriz": true, "apelido": "Matriz", "address": { }, "contatos": [ ] }
  ]
}
```

Consequências para a tela de cadastro:
- `tipo` é **obrigatório** e precisa ser escolhido **antes** do documento (ele decide se
  o campo é CPF ou CNPJ, e se a seção de filiais existe).
- `branches` é obrigatório e não pode ser vazio: no mínimo a matriz.
- `systemsPublicIds` agora é **opcional** (era obrigatório). Pode omitir ou mandar `[]` —
  cliente pode ser cadastrado antes de se saber quais produtos usa.
- **`enabled` no POST é ignorado**: o cliente sempre nasce ativo. Não exponha o campo no
  formulário de criação.

### 4.3 `ClientResponse` ganhou campos

Novos: `tipo`, `nomeFantasia`, `regimeTributario`, `branches[]`. A listagem e a tela de
detalhe precisam ser reajustadas — em especial, **o documento não está no cliente**, está
em `branches[].documento`. Para exibir "o CNPJ do cliente" numa listagem, use o documento
da filial com `isMatriz: true`.

### 4.4 Filiais não se editam pelo cliente

`PATCH /clients/{publicId}` **ignora** qualquer coisa relacionada a filiais. Toda
manutenção de filial (incluindo a matriz) é pelo sub-recurso
`/clients/{publicId}/branches`.

---

## 5. Endpoints

Resumo, com a role exigida — o `@PreAuthorize` **não aparece no OpenAPI**, então esta
tabela é a única fonte:

| Método | Path | Role | Sucesso |
|---|---|---|---|
| `GET` | `/clients` | USER | 200 `ClientResponse[]` |
| `GET` | `/clients/{publicId}` | USER | 200 `ClientResponse` |
| `POST` | `/clients/` | USER | 201 `ClientResponse` |
| `PATCH` | `/clients/{publicId}` | USER | 200 `ClientResponse` |
| `DELETE` | `/clients/{publicId}` | **ADMIN** | 204 |
| `GET` | `/clients/lookup?documento=` | USER | 200 `CnpjLookupResponse` |
| `GET` | `/clients/{publicId}/branches` | USER | 200 `BranchResponse[]` |
| `POST` | `/clients/{publicId}/branches` | USER | 201 `BranchResponse` |
| `PATCH` | `/clients/{publicId}/branches/{branchId}` | USER | 200 `BranchResponse` |
| `POST` | `/clients/{publicId}/branches/{branchId}/deactivate` | **ADMIN** | 204 |
| `POST` | `/clients/{publicId}/branches/{branchId}/activate` | **ADMIN** | 204 |

Os quatro endpoints ADMIN devem ter o botão **oculto ou desabilitado** para `USER` —
o front já conhece as roles do usuário logado pela sessão.

---

## 6. Tela: Listagem de clientes

```
GET /clients?name=&status=&tipo=&documento=&systems=&page=&size=&sort=
```

| Param | Tipo | Comportamento |
|---|---|---|
| `name` | string | `LIKE` case-insensitive, parcial. |
| `status` | `ATIVO \| INATIVO \| TODOS` | Omitido = todos. |
| `tipo` | `PJ \| PF` | **Novo.** Igualdade exata. |
| `documento` | string | **Novo.** Aceita com máscara (normaliza). Igualdade exata contra **qualquer filial** do cliente — inclusive não-matriz. |
| `systems` | UUID[] | Repetido (`systems=a&systems=b`) ou separado por vírgula. |
| `page` | int | **Novo.** Índice da página, base 0. |
| `size` | int | **Novo.** Itens por página. Padrão `12`. |
| `sort` | string | **Novo.** Formato Spring: `name,asc` (padrão) ou `createdAt,desc`. Ignorado sem `page`/`size`. |

Todos os filtros são combináveis (`AND`).

### Paginação é opcional — e ela muda o formato da resposta

Compatibilidade preservada de propósito: **sem `page` e sem `size` a resposta continua
sendo o array simples de sempre.** Basta enviar um dos dois para receber o envelope
`Page` do Spring.

| Requisição | Corpo da resposta |
|---|---|
| `GET /clients?name=acme` | `ClientResponse[]` — array cru, tudo que casa com o filtro |
| `GET /clients?name=acme&page=0` | `Page<ClientResponse>` — 12 itens, ordenados por `name` |
| `GET /clients?size=50` | `Page<ClientResponse>` — página 0 com 50 itens |

Envelope quando paginado:

```json
{
  "content": [ /* ClientResponse[], mesmo objeto de sempre */ ],
  "totalElements": 137,
  "totalPages": 12,
  "number": 0,
  "size": 12,
  "numberOfElements": 12,
  "first": true,
  "last": false,
  "empty": false
}
```

Os itens de `content` são o **`ClientResponse` completo** — com `branches`, `contatos`
e `systems`, nada de payload reduzido. `totalElements` conta os clientes que casam com
o filtro, não as linhas do join: filtrar por `documento` de uma filial devolve o cliente
uma vez só.

Resposta sem paginação (`ClientResponse[]`):

```json
[{
  "publicId": "3f0c...",
  "tipo": "PJ",
  "name": "Empresa X",
  "nomeFantasia": "X",
  "regimeTributario": "SIMPLES_NACIONAL",
  "enabled": true,
  "createdAt": "2026-07-29T13:45:12.123456Z",
  "systems": [{ "publicId": "aa..", "name": "ERP", "enabled": true }],
  "branches": [{
    "publicId": "9b1e...",
    "documento": "11222333000181",
    "isMatriz": true,
    "apelido": "Matriz",
    "inscricaoEstadual": null,
    "inscricaoMunicipal": null,
    "address": { "cep": "01001000", "logradouro": "Praça da Sé", "numero": "100",
                 "complemento": null, "bairro": "Centro", "cidade": "São Paulo", "uf": "SP" },
    "contatos": [{ "publicId": "c1..", "tipo": "EMAIL", "valor": "a@b.com",
                   "descricao": "Financeiro", "principal": true }],
    "enabled": true
  }]
}]
```

Notas para a tela:
- `branches` vem **sempre** como array (lista vazia, nunca `null`).
- `address` pode ser `null`; cada campo dentro dele também.
- Sugestão de colunas: nome, documento da matriz, tipo, nº de filiais ativas, status.
- O filtro `documento` casando com filial não-matriz é útil: buscar pelo CNPJ de uma
  unidade encontra o cliente dono dela.

---

## 7. Tela: Cadastro de cliente

Fluxo desenhado em três etapas: **escolher o tipo → consultar CNPJ (opcional) → revisar e salvar.**

### 7.1 Etapa 1 — tipo

`tipo` primeiro, porque decide todo o resto:

| | `PJ` | `PF` |
|---|---|---|
| Documento da matriz | CNPJ (14 dígitos ou alfanumérico) | CPF (11 dígitos) |
| Consulta automática | disponível | **não existe** (lookup é só CNPJ) |
| Seção de filiais | disponível | **oculta** — PF só tem a matriz |
| `regimeTributario` | faz sentido | normalmente vazio |

### 7.2 Etapa 2 — consulta de CNPJ (opcional, só `PJ`)

```
GET /clients/lookup?documento=07526557011659
```

**Não persiste nada.** Devolve os dados já no formato do `POST /clients/`, para
pré-preencher o formulário. O usuário revisa e corrige antes de salvar.

```json
{
  "tipo": "PJ",
  "name": "AMBEV S.A.",
  "nomeFantasia": "Ambev",
  "regimeTributario": null,
  "situacaoCadastral": "Ativa",
  "situacaoAtiva": true,
  "branches": [{
    "documento": "07526557011659",
    "isMatriz": false,
    "apelido": "Filial Manaus",
    "inscricaoEstadual": null,
    "inscricaoMunicipal": null,
    "address": { "cep": "69075000", "logradouro": "...", "numero": "...",
                 "complemento": null, "bairro": "...", "cidade": "Manaus", "uf": "AM" },
    "contatos": [{ "tipo": "TELEFONE", "valor": "9236142000", "descricao": null, "principal": null },
                 { "tipo": "EMAIL", "valor": "x@ambev.com.br", "descricao": "Corporativo", "principal": null }]
  }]
}
```

**Como consumir — pontos que o front precisa tratar:**

1. **`branches` vem com um item só**, correspondente ao CNPJ consultado. Copie direto
   para o formulário (o shape é o mesmo do `BranchRequest` do POST).
2. **`isMatriz` reflete a realidade da Receita**, e vem `false` se o usuário consultou
   uma filial. Mas o `POST /clients/` exige **exatamente uma matriz**: se o cadastro
   está sendo criado a partir de uma filial, o front precisa marcar `isMatriz: true`
   nessa única filial antes de salvar (ou orientar o usuário a consultar a matriz).
3. **`nomeFantasia` só vem preenchido quando o CNPJ consultado é matriz.** Consultando
   filial, vem `null` — deixe o campo editável e vazio.
4. **Campos que voltam sempre em branco** (limitação do provedor público, não é bug):
   - `inscricaoEstadual` — a API pública não devolve o bloco de inscrições.
   - `inscricaoMunicipal` — não existe no contrato do provedor.
   - `contatos[].principal` — vem `null` em todos. **O usuário deve escolher um**, e no
     máximo um, senão o POST volta 422.
   Marque esses campos na UI como "preenchimento manual".
5. **`regimeTributario`** só é preenchido quando o provedor afirma: `MEI` ou
   `SIMPLES_NACIONAL`. `LUCRO_PRESUMIDO`/`LUCRO_REAL` **nunca** vêm — deixe o select
   aberto para o usuário.
6. **`situacaoCadastral` e `situacaoAtiva` são aviso, não bloqueio.** CNPJ suspenso,
   inapto ou baixado vem preenchido do mesmo jeito, com `situacaoAtiva: false`. Mostre
   um alerta amarelo com o texto de `situacaoCadastral` e **deixe o cadastro seguir** —
   quem decide é o atendente.
7. **Rate limit do provedor: 5 consultas por minuto por IP**, compartilhado por toda a
   equipe. Portanto:
   - Não dispare a consulta a cada tecla. Só no `blur` do campo ou em botão explícito
     ("Buscar dados na Receita").
   - Valide o CNPJ no cliente antes de chamar (checksum) — o backend também valida e
     devolve 400 sem gastar cota, mas evitar o round-trip é melhor.
   - Em **429**, mostre "Aguarde um minuto e tente novamente" e mantenha o formulário
     editável.
   - Em **503**, mostre "Preencha manualmente" e siga — o cadastro manual é o fallback.
   - Respostas boas ficam em cache no backend por 24h, então repetir a mesma consulta
     não consome cota.

Erros do lookup:

| Status | `message` | UI |
|---|---|---|
| 400 | `O CNPJ é obrigatório` | erro no campo |
| 400 | `CNPJ inválido` | erro no campo (checksum) |
| 404 | `CNPJ não encontrado na Receita Federal: {cnpj}` | erro no campo, sugerir cadastro manual |
| 429 | `Limite de consultas de CNPJ atingido. Aguarde um minuto e tente novamente.` | toast, formulário segue editável |
| 503 | `Serviço de consulta de CNPJ indisponível no momento. Preencha o cadastro manualmente.` | toast, formulário segue editável |

### 7.3 Etapa 3 — salvar

```
POST /clients/
```

```ts
type ClientRequest = {
  tipo: 'PJ' | 'PF';                    // obrigatório
  name: string;                         // obrigatório, 1..100
  nomeFantasia?: string | null;         // até 100
  regimeTributario?: RegimeTributario | null;
  systemsPublicIds?: string[] | null;   // opcional; [] aceito
  branches: BranchRequest[];            // obrigatório, >= 1, exatamente 1 com isMatriz
  // enabled: IGNORADO no create
};

type BranchRequest = {
  documento: string;                    // obrigatório no create; CPF ou CNPJ conforme o tipo
  isMatriz?: boolean | null;            // default false
  apelido?: string | null;              // até 100
  inscricaoEstadual?: string | null;    // até 20
  inscricaoMunicipal?: string | null;   // até 20
  address?: AddressRequest | null;      // todo opcional
  contatos?: ContactRequest[] | null;   // no máximo 1 com principal: true
};
```

Validações do front **antes** de habilitar o submit (espelham o backend):

- `tipo` selecionado; `name` não vazio e ≤ 100.
- ao menos uma filial, e **exatamente uma** com `isMatriz: true`.
  - Em `PF`: force uma filial só e marque `isMatriz: true` (o backend promove
    automaticamente, mas não deixe o usuário adicionar uma segunda).
- `documento` de cada filial: obrigatório, checksum válido para o `tipo`, e **sem
  repetição dentro do próprio payload**.
- por filial, no máximo um contato com `principal: true`.
- cada contato precisa de `tipo` e `valor` (≤ 150).
- tamanhos: `apelido` 100, inscrições 20, `cep` 8 dígitos, `uf` 2, `logradouro` 150,
  `numero` 20, `complemento`/`bairro`/`cidade` 100, `descricao` do contato 100.

Erros do POST:

| Status | `message` / `fieldErrors` | Causa | UI |
|---|---|---|---|
| 400 | `fieldErrors["branches[0].documento"] = "CNPJ inválido"` (ou `"CPF inválido"`) | checksum | erro no campo do documento daquela filial |
| 400 | `fieldErrors["name"]`, `fieldErrors["branches"]`, `fieldErrors["branches[0].contatos[0].valor"]`… | Bean Validation | erro no campo correspondente |
| 409 | `Client with name: {name} already exists` | nome duplicado | erro no campo `name` |
| 409 | `Documento: {doc} already exists` | documento já usado por **outro cliente** (ou filial inativa) | erro no campo do documento; extraia o doc da mensagem para achar a filial |
| 422 | `Informe ao menos a matriz` | `branches` vazio | erro global |
| 422 | `O cliente deve ter exatamente uma filial marcada como matriz` | zero ou 2+ matrizes | erro global |
| 422 | `Cliente pessoa física deve ter apenas a matriz` | PF com 2+ filiais | erro global |
| 422 | `O documento é obrigatório para toda filial nova` | filial sem documento | erro no campo |
| 422 | `Documento repetido no mesmo cadastro: {doc}` | duplicado no payload | erro no campo |
| 422 | `A filial deve ter no máximo um contato principal` | 2+ principais na mesma filial | erro na seção de contatos |
| 404 | `Um ou mais sistemas não foram encontrados para os IDs fornecidos.` | algum UUID de `systemsPublicIds` não existe | erro no select de sistemas (a mensagem não diz **qual** id falhou) |

> **Atenção à assimetria 400/422:** documento com checksum inválido responde **400 com
> `fieldErrors`** aqui, e **422 com mensagem solta** no `POST /branches`. É intencional
> (aqui é Bean Validation, lá é regra de serviço). O tratamento de erro precisa cobrir
> os dois formatos para a mesma mensagem.

---

## 8. Tela: Edição de cliente

```
PATCH /clients/{publicId}
```

Semântica PATCH real: **só os campos presentes e não nulos são aplicados.** Omitir um
campo o preserva. Não mande o objeto inteiro se só um campo mudou.

```ts
type ClientUpdateRequest = {
  tipo?: 'PJ' | 'PF';
  name?: string;                 // até 100
  nomeFantasia?: string;         // até 100
  regimeTributario?: RegimeTributario;
  systemsPublicIds?: string[];   // presente SUBSTITUI a lista; [] remove todos
  enabled?: boolean;             // aqui SIM é aplicado (diferente do create)
};
```

O que **não** se faz por aqui:
- **Filiais.** `branches` não existe neste DTO e é ignorado. Use o sub-recurso.

> **Gap conhecido do backend:** o PATCH **não valida nome duplicado**, e a coluna
> `clients.name` **não tem `UNIQUE`** no banco. Renomear um cliente para um nome já
> existente é aceito com 200 e cria dois clientes homônimos — mesmo que o `POST`
> rejeite isso com 409. Se a UI não pode conviver com homônimos, valide no front
> (buscar `GET /clients?name=` antes de salvar) e reporte ao backend.

Regras:

| Status | `message` | Causa | UI |
|---|---|---|---|
| 404 | `Client with public ID: {uuid} not found` | id inexistente | redirecionar para a listagem |
| 422 | `Cliente pessoa física deve ter apenas a matriz; inative as demais filiais antes de trocar o tipo` | mudar para `PF` com 2+ filiais ativas | erro global + link para a aba de filiais |
| 404 | `Um ou mais sistemas não foram encontrados para os IDs fornecidos.` | UUID inválido em `systemsPublicIds` | erro no select de sistemas |

**Ativar/inativar cliente:** existem dois caminhos e eles têm roles diferentes.
- `DELETE /clients/{publicId}` → 204, soft delete (`enabled: false`). **ADMIN.**
- `PATCH /clients/{publicId}` com `{ "enabled": false }` ou `{ "enabled": true }` → 200. **USER.**

Reativar cliente só é possível pelo PATCH. Escolha um caminho e seja consistente na UI;
a recomendação é usar o PATCH para o toggle de status e não expor o DELETE.

> Cliente inativo **bloqueia** criação e edição de filiais (422 `Client is not active`),
> mas **não** bloqueia a listagem de filiais.

---

## 9. Tela: Filiais do cliente

Sub-recurso. Sugestão de UI: aba dentro do detalhe do cliente. Oculte a aba inteira
quando `tipo === 'PF'`.

### 9.1 Listar

```
GET /clients/{publicId}/branches?status=ATIVO|INATIVO|TODOS
```

Omitir `status` = todas (ativas e inativas). Funciona também para cliente inativo.
Resposta: `BranchResponse[]` (shape na seção 6).

- 404 `Client with public ID: {uuid} not found` se o cliente não existe.

### 9.2 Criar filial

```
POST /clients/{publicId}/branches
```

Body: `BranchRequest` (mesmo shape do create do cliente).

Regras específicas deste endpoint — **todas 422**, exceto onde indicado:

| `message` | Causa | Prevenção na UI |
|---|---|---|
| `Client is not active` | cliente inativo | esconder o botão "Nova filial" |
| `Cliente pessoa física deve ter apenas a matriz` | cliente é `PF` | esconder a aba de filiais para PF |
| `A matriz não pode ser definida no cadastro da filial; promova a filial depois de criada` | mandou `isMatriz: true` | **nunca** envie `isMatriz` no create; não exponha o campo |
| `O documento é obrigatório para toda filial nova` | documento ausente | campo obrigatório no form |
| `CNPJ inválido` / `CPF inválido` | checksum (tipo vem do **cliente persistido**) | validar no cliente |
| `A filial deve ter no máximo um contato principal` | 2+ principais | radio, não checkbox |
| **409** `Documento: {doc} already exists` | documento em uso em qualquer filial da base | mensagem clara: "documento já cadastrado em outro cliente" |
| **404** `Client with public ID: {uuid} not found` | cliente inexistente | — |

### 9.3 Editar filial

```
PATCH /clients/{publicId}/branches/{branchId}
```

```ts
type BranchUpdateRequest = {
  documento?: string;              // validado só se presente
  isMatriz?: boolean;              // true = promover (ver 9.4); false = 422 se já é matriz
  apelido?: string;
  inscricaoEstadual?: string;
  inscricaoMunicipal?: string;
  address?: AddressRequest;
  contatos?: ContactRequest[];     // CUIDADO: substitui a lista INTEIRA
  // não existe `enabled`: ativar/inativar tem endpoint próprio
};
```

**⚠️ `contatos` substitui a lista inteira.** Omitir não altera nada; enviar substitui.
Para adicionar, editar ou remover **um** contato, envie **todos** os contatos que devem
permanecer. Mandar só o contato editado **apaga os outros**. Os contatos são recriados,
então **`publicId` dos contatos muda** após um PATCH que envie `contatos` — não guarde
essas referências entre operações.

O mesmo vale para `address`: é um objeto embutido, mande-o completo.

| Status | `message` | Causa |
|---|---|---|
| 404 | `Branch with public ID: {uuid} not found` | filial inexistente **ou pertencente a outro cliente** (é 404 de propósito, não 403) |
| 404 | `Client with public ID: {uuid} not found` | cliente inexistente |
| 422 | `Client is not active` | cliente inativo |
| 422 | `CNPJ inválido` / `CPF inválido` | checksum do documento enviado |
| 422 | `A matriz não pode ser rebaixada; promova outra filial a matriz` | `isMatriz: false` na matriz atual |
| 422 | `Uma filial inativa não pode ser promovida a matriz` | `isMatriz: true` em filial inativa |
| 422 | `A filial deve ter no máximo um contato principal` | 2+ principais |
| 409 | `Documento: {doc} already exists` | documento de outra filial (a própria filial é excluída da checagem — reenviar o mesmo documento é permitido) |

### 9.4 Transferir a matriz

Não há endpoint dedicado: é o `PATCH` da filial que **vai virar** matriz, com
`isMatriz: true`. O backend rebaixa a matriz anterior na mesma transação.

```
PATCH /clients/{clientId}/branches/{novaMatrizId}   { "isMatriz": true }
```

Fluxo na UI:
1. Botão "Definir como matriz" na linha de cada filial **ativa** e **não-matriz**.
2. Confirmação explícita: "A filial X passa a ser a matriz, e Y deixa de ser."
3. Depois do 200, **recarregue a lista** — duas linhas mudaram, não só a editada.

### 9.5 Inativar / reativar filial (ADMIN)

```
POST /clients/{publicId}/branches/{branchId}/deactivate   → 204
POST /clients/{publicId}/branches/{branchId}/activate     → 204
```

Sem body. Ambos são **ADMIN**. Respostas 204 sem corpo — recarregue a lista depois.

| Status | `message` | Causa |
|---|---|---|
| 422 | `A matriz não pode ser inativada; promova outra filial a matriz antes` | deactivate na matriz |
| 422 | `Cliente pessoa física deve ter apenas a matriz; não é possível reativar uma segunda filial` | activate de 2ª filial de PF |
| 422 | `Client is not active` | cliente inativo |
| 404 | `Branch with public ID: {uuid} not found` | filial de outro cliente |

Comportamentos que a UI pode explorar:
- **`activate` é idempotente**: filial já ativa devolve 204 sem erro. Não precisa
  checar estado antes.
- **`deactivate` é soft delete**: o `documento` continua ocupando o `UNIQUE` global.
  Se o usuário inativar uma filial esperando liberar o CNPJ para outro cliente, vai
  tomar 409. Vale um texto explicativo no diálogo de confirmação.

---

## 10. Matriz de habilitação de ações (filiais)

Estado da filial × ação disponível. Use isto para decidir botão visível/habilitado —
sem isso o usuário clica em coisa que sempre volta 422.

| Ação | Matriz ativa | Filial ativa | Filial inativa | Cliente inativo | Cliente PF |
|---|---|---|---|---|---|
| Editar dados (`PATCH`) | ✅ | ✅ | ✅ | ❌ 422 | ✅ (só a matriz) |
| Definir como matriz | — (já é) | ✅ | ❌ 422 | ❌ 422 | — |
| Rebaixar (`isMatriz: false`) | ❌ 422 sempre | — | — | — | — |
| Inativar (ADMIN) | ❌ 422 sempre | ✅ | (idempotente na prática) | ❌ 422 | ❌ (só tem matriz) |
| Reativar (ADMIN) | — | ✅ 204 no-op | ✅ | ❌ 422 | ❌ 422 se já há uma ativa |
| Criar nova filial | — | — | — | ❌ 422 | ❌ 422 |

Regra derivada: **a matriz nunca pode ser inativada nem rebaixada diretamente.** Para
"desligar" a matriz, o usuário precisa primeiro promover outra filial e depois inativar
a antiga. Guie esse fluxo na interface.

---

## 11. Tabela consolidada de status HTTP

| Status | Significado no backend | Tratamento padrão no front |
|---|---|---|
| 400 | Bean Validation (com `fieldErrors`) ou entrada inválida em query param (sem `fieldErrors`) | erro por campo; se não houver `fieldErrors`, toast |
| 401 | Sem cookie válido | tentar `refresh` uma vez, depois login |
| 403 | Role insuficiente (endpoints ADMIN) | não deveria acontecer se a UI esconde os botões; toast genérico |
| 404 | Cliente, filial ou sistema inexistente; **ou filial de outro cliente** | toast + recarregar/redirecionar |
| 409 | Documento ou nome já existente; violação de integridade | erro no campo correspondente |
| 422 | Regra de negócio | toast/alerta com `message` — as mensagens já são exibíveis em pt-BR |
| 429 | Rate limit do provedor de CNPJ (só no lookup) | "aguarde um minuto"; formulário segue editável |
| 503 | Provedor de CNPJ indisponível (só no lookup) | "preencha manualmente"; formulário segue editável |

As mensagens de 422 são escritas em pt-BR e **podem ser exibidas direto ao usuário**.
As de 404/409 são em inglês e contêm IDs — **não exiba cru**; use texto próprio e, se
precisar, extraia o documento/id da mensagem para destacar o campo.

---

## 12. Checklist de aceite

Marque cada item testando na tela, não só lendo o código.

**Integração**
- [ ] Todos os requests mandam `credentials: 'include'`; nenhum header além de
      `Content-Type`/`Accept`.
- [ ] Nenhum lugar do código lê ou grava token em storage.
- [ ] 401 dispara `refresh` uma vez e repete o request original.
- [ ] `POST /clients/` usa a barra final.

**Cadastro**
- [ ] `tipo` é escolhido antes do documento e alterna a máscara CPF/CNPJ.
- [ ] `PF` esconde a seção de filiais e envia uma única filial com `isMatriz: true`.
- [ ] Sempre exatamente uma filial marcada como matriz antes de habilitar o submit.
- [ ] Contato principal é radio (no máximo um por filial).
- [ ] `enabled` não aparece no formulário de criação.
- [ ] Erro 400 com `fieldErrors["branches[0].documento"]` aparece no campo certo da
      filial certa.

**Lookup**
- [ ] Consulta só em `blur`/botão, nunca a cada tecla.
- [ ] Checksum validado no cliente antes de chamar.
- [ ] `situacaoAtiva: false` mostra aviso e **não** bloqueia o cadastro.
- [ ] `inscricaoEstadual`, `inscricaoMunicipal` e o contato principal ficam sinalizados
      como preenchimento manual.
- [ ] Consultando uma filial (`isMatriz: false`), o front resolve a matriz antes do POST.
- [ ] 429 e 503 mantêm o formulário editável.

**Filiais**
- [ ] `PATCH` de contatos envia a lista completa (teste: editar 1 de 3 contatos e
      confirmar que os 3 continuam lá).
- [ ] Nenhum `publicId` de contato é reaproveitado depois de um PATCH com `contatos`.
- [ ] Botão "definir como matriz" só em filial ativa e não-matriz; recarrega a lista
      após o 200.
- [ ] Inativar/reativar só aparece para ADMIN.
- [ ] Nenhum botão exposto viola a matriz da seção 10.

---

## 13. Como regenerar este pacote

No repositório do backend, com a stack e a aplicação rodando no perfil `dev`:

```bash
docker compose --env-file .env -f Docker/docker-compose.yaml up -d
./mvnw spring-boot:run

curl -s http://localhost:8080/v3/api-docs | python3 -m json.tool > docs/api/openapi.json
npx -y openapi-typescript@7 docs/api/openapi.json -o docs/api/api.d.ts
```

`/v3/api-docs/**` é público no perfil `dev` e **desabilitado em prod** — o export é
sempre local. Swagger UI: `http://localhost:8080/swagger-ui.html`.

O `CONTRATO-CLIENTES.md` e o `client-api.ts` são escritos à mão: atualize-os quando
mudar regra de negócio, role de endpoint ou mensagem de erro.
