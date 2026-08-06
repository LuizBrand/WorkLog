/**
 * Tipos e contratos do módulo de Clientes da WorkLog API.
 *
 * Escrito à mão de propósito: o `api.d.ts` gerado do OpenAPI marca como opcional
 * todo campo que não tem `@NotNull` no backend — `name`, `branches` e `valor` do
 * contato saem opcionais lá, mas a API rejeita com 400 se vierem vazios.
 * Para o módulo de clientes, use ESTE arquivo.
 *
 * Regras de negócio, erros e fluxo de tela: ver CONTRATO-CLIENTES.md.
 * Backend: branch feat/client-registration-expasion (fases 1–6).
 */

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

export const ClientType = {
  PJ: 'PJ',
  PF: 'PF',
} as const;
export type ClientType = (typeof ClientType)[keyof typeof ClientType];

export const RegimeTributario = {
  SIMPLES_NACIONAL: 'SIMPLES_NACIONAL',
  LUCRO_PRESUMIDO: 'LUCRO_PRESUMIDO',
  LUCRO_REAL: 'LUCRO_REAL',
  MEI: 'MEI',
  IMUNE: 'IMUNE',
  ISENTO: 'ISENTO',
} as const;
export type RegimeTributario = (typeof RegimeTributario)[keyof typeof RegimeTributario];

export const ContactType = {
  EMAIL: 'EMAIL',
  TELEFONE: 'TELEFONE',
  CELULAR: 'CELULAR',
  WHATSAPP: 'WHATSAPP',
} as const;
export type ContactType = (typeof ContactType)[keyof typeof ContactType];

export const StatusFiltro = {
  ATIVO: 'ATIVO',
  INATIVO: 'INATIVO',
  TODOS: 'TODOS',
} as const;
export type StatusFiltro = (typeof StatusFiltro)[keyof typeof StatusFiltro];

/** Labels para exibição. O backend não envia label. */
export const CLIENT_TYPE_LABEL: Record<ClientType, string> = {
  PJ: 'Pessoa Jurídica',
  PF: 'Pessoa Física',
};

export const REGIME_TRIBUTARIO_LABEL: Record<RegimeTributario, string> = {
  SIMPLES_NACIONAL: 'Simples Nacional',
  LUCRO_PRESUMIDO: 'Lucro Presumido',
  LUCRO_REAL: 'Lucro Real',
  MEI: 'MEI',
  IMUNE: 'Imune',
  ISENTO: 'Isento',
};

export const CONTACT_TYPE_LABEL: Record<ContactType, string> = {
  EMAIL: 'E-mail',
  TELEFONE: 'Telefone',
  CELULAR: 'Celular',
  WHATSAPP: 'WhatsApp',
};

// ---------------------------------------------------------------------------
// Limites de tamanho (espelham as anotações de validação do backend)
// ---------------------------------------------------------------------------

export const MAX_LENGTH = {
  clientName: 100,
  nomeFantasia: 100,
  apelido: 100,
  inscricaoEstadual: 20,
  inscricaoMunicipal: 20,
  contatoValor: 150,
  contatoDescricao: 100,
  cep: 8,
  logradouro: 150,
  numero: 20,
  complemento: 100,
  bairro: 100,
  cidade: 100,
  uf: 2,
} as const;

// ---------------------------------------------------------------------------
// Requests
// ---------------------------------------------------------------------------

/** Endereço da filial. Todos os campos são opcionais. O backend normaliza `cep` (só dígitos) e `uf` (maiúsculas). */
export interface AddressRequest {
  cep?: string | null;
  logradouro?: string | null;
  numero?: string | null;
  complemento?: string | null;
  bairro?: string | null;
  cidade?: string | null;
  uf?: string | null;
}

/** Contato da filial. No máximo UM contato com `principal: true` por filial. */
export interface ContactRequest {
  tipo: ContactType;
  /** Obrigatório e não vazio. O formato não é validado por tipo pelo backend. */
  valor: string;
  descricao?: string | null;
  /** Ausente/null equivale a false. */
  principal?: boolean | null;
}

/**
 * Filial no cadastro do cliente e no POST do sub-recurso.
 *
 * `documento`: CPF se o cliente é PF, CNPJ se é PJ — com checksum válido.
 * Pode ir com máscara; o backend remove e aplica maiúsculas (CNPJ alfanumérico).
 */
export interface BranchRequest {
  documento: string;
  /**
   * Só use no POST /clients (exatamente uma filial com true).
   * No POST /clients/{id}/branches, enviar `true` é rejeitado com 422 —
   * promover é operação do PATCH.
   */
  isMatriz?: boolean | null;
  apelido?: string | null;
  inscricaoEstadual?: string | null;
  inscricaoMunicipal?: string | null;
  address?: AddressRequest | null;
  contatos?: ContactRequest[] | null;
}

/**
 * POST /clients/  (atenção à barra final no path)
 *
 * `enabled` não existe aqui: o backend ignora o campo no create e o cliente
 * sempre nasce ativo.
 */
export interface ClientRequest {
  tipo: ClientType;
  /** Obrigatório, não vazio, até 100. Único: nome repetido volta 409. */
  name: string;
  nomeFantasia?: string | null;
  regimeTributario?: RegimeTributario | null;
  /** Opcional. Omitido ou `[]` cria cliente sem sistema associado. */
  systemsPublicIds?: string[] | null;
  /** Obrigatório, não vazio, com EXATAMENTE uma filial `isMatriz: true`. */
  branches: BranchRequest[];
}

/**
 * PATCH /clients/{publicId} — só os campos presentes e não nulos são aplicados.
 *
 * Filiais NÃO são editadas por aqui; use o sub-recurso.
 * `systemsPublicIds` presente substitui a lista inteira (`[]` remove todas).
 */
export interface ClientUpdateRequest {
  tipo?: ClientType;
  name?: string;
  nomeFantasia?: string;
  regimeTributario?: RegimeTributario;
  systemsPublicIds?: string[];
  /** Diferente do create: aqui `enabled` é aplicado (inativa/reativa o cliente). */
  enabled?: boolean;
}

/**
 * PATCH /clients/{publicId}/branches/{branchId} — só os campos presentes e não
 * nulos são aplicados.
 *
 * ⚠️ `contatos` presente SUBSTITUI a lista inteira. Para editar um contato,
 * envie todos os que devem permanecer. Os contatos são recriados, então os
 * `publicId` deles mudam.
 *
 * Não há `enabled`: ativar/inativar tem endpoint próprio.
 */
export interface BranchUpdateRequest {
  documento?: string;
  /** `true` promove esta filial e rebaixa a matriz atual. `false` na matriz atual → 422. */
  isMatriz?: boolean;
  apelido?: string;
  inscricaoEstadual?: string;
  inscricaoMunicipal?: string;
  address?: AddressRequest;
  contatos?: ContactRequest[];
}

/** Query params de GET /clients. Todos opcionais e combináveis (AND). */
export interface ClientFiltersParams {
  /** LIKE case-insensitive, parcial. */
  name?: string;
  /** Omitido = todos. */
  status?: StatusFiltro;
  tipo?: ClientType;
  /** Aceita com máscara. Igualdade exata contra qualquer filial do cliente. */
  documento?: string;
  systems?: string[];
  /**
   * Índice da página, base 0. Enviar `page` OU `size` muda o formato da
   * resposta de `ClientResponse[]` para `Page<ClientResponse>`.
   */
  page?: number;
  /** Itens por página. Padrão 12. Mesmo efeito de `page` sobre a resposta. */
  size?: number;
  /**
   * Ordenação, formato Spring: "name,asc" ou "createdAt,desc". Padrão
   * "name,asc". Só tem efeito junto com `page` ou `size`.
   */
  sort?: string;
}

/**
 * Envelope de página do Spring Data, usado por GET /clients quando `page` ou
 * `size` estão presentes na query string.
 */
export interface Page<T> {
  content: T[];
  /** Total de registros que casam com o filtro, ignorando a paginação. */
  totalElements: number;
  totalPages: number;
  /** Índice da página atual, base 0. */
  number: number;
  /** Tamanho da página pedido. */
  size: number;
  /** Quantidade de itens nesta página — menor que `size` na última. */
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// ---------------------------------------------------------------------------
// Responses
// ---------------------------------------------------------------------------

export interface AddressResponse {
  cep: string | null;
  logradouro: string | null;
  numero: string | null;
  complemento: string | null;
  bairro: string | null;
  cidade: string | null;
  uf: string | null;
}

export interface ContactResponse {
  publicId: string;
  tipo: ContactType;
  valor: string;
  descricao: string | null;
  principal: boolean;
}

export interface BranchResponse {
  publicId: string;
  /** Null apenas em filiais de legado, criadas antes da fase 1. */
  documento: string | null;
  isMatriz: boolean;
  apelido: string | null;
  inscricaoEstadual: string | null;
  inscricaoMunicipal: string | null;
  address: AddressResponse | null;
  contatos: ContactResponse[];
  enabled: boolean;
}

export interface SystemResponse {
  publicId: string;
  name: string;
  enabled: boolean;
}

export interface ClientResponse {
  publicId: string;
  tipo: ClientType;
  name: string;
  nomeFantasia: string | null;
  regimeTributario: RegimeTributario | null;
  enabled: boolean;
  /** ISO-8601 com offset UTC: "2026-07-29T13:45:12.123456Z" */
  createdAt: string;
  systems: SystemResponse[];
  /** Sempre array; lista vazia em vez de null. O documento do cliente está aqui. */
  branches: BranchResponse[];
}

/**
 * GET /clients/lookup — dados da Receita já no formato do formulário.
 * Nada é persistido.
 *
 * Campos que voltam SEMPRE em branco com o provedor público:
 * `branches[].inscricaoEstadual`, `branches[].inscricaoMunicipal` e
 * `branches[].contatos[].principal`. `regimeTributario` só vem quando o
 * provedor afirma MEI ou Simples Nacional.
 */
export interface CnpjLookupResponse {
  tipo: 'PJ';
  /** Razão social. */
  name: string | null;
  /** Só preenchido quando o CNPJ consultado é matriz. */
  nomeFantasia: string | null;
  regimeTributario: RegimeTributario | null;
  /** Texto da situação na Receita: "Ativa", "Suspensa", "Baixada"... */
  situacaoCadastral: string | null;
  /** false = CNPJ não ativo. É AVISO, não bloqueio: o cadastro pode seguir. */
  situacaoAtiva: boolean;
  /** Sempre um item, correspondente ao CNPJ consultado. */
  branches: BranchRequest[];
}

// ---------------------------------------------------------------------------
// Erros
// ---------------------------------------------------------------------------

/** Corpo de erro único da API, em todos os status. */
export interface ApiExceptionResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  /**
   * Presente SOMENTE em 400 de Bean Validation. A chave é o caminho do campo,
   * com índice quando é array: "branches[0].documento",
   * "branches[1].contatos[0].valor".
   */
  fieldErrors?: Record<string, string>;
}

/** true quando o erro deve ser renderizado por campo, e não em toast. */
export function hasFieldErrors(
  body: ApiExceptionResponse,
): body is ApiExceptionResponse & { fieldErrors: Record<string, string> } {
  return !!body.fieldErrors && Object.keys(body.fieldErrors).length > 0;
}

/**
 * Mensagens de 422 são regras de negócio em pt-BR e podem ser exibidas direto.
 * As de 404/409 são em inglês e contêm IDs — não exiba cru.
 */
export function isDisplayableMessage(status: number): boolean {
  return status === 400 || status === 422 || status === 429 || status === 503;
}

// ---------------------------------------------------------------------------
// Endpoints
// ---------------------------------------------------------------------------

/**
 * Autenticação é por cookie httpOnly (`worklog_access`), não por header.
 * TODO request precisa de `credentials: 'include'`.
 * Não mande header além de Content-Type/Accept: o CORS bloqueia no preflight.
 */
export const CLIENT_ENDPOINTS = {
  list: () => `/clients`,
  byId: (publicId: string) => `/clients/${publicId}`,
  /** A barra final é obrigatória: o backend mapeia literalmente "/clients/". */
  create: () => `/clients/`,
  update: (publicId: string) => `/clients/${publicId}`,
  /** ADMIN. Soft delete. Para reativar, use o PATCH com { enabled: true }. */
  softDelete: (publicId: string) => `/clients/${publicId}`,
  lookupCnpj: (documento: string) =>
    `/clients/lookup?documento=${encodeURIComponent(documento)}`,

  branches: (publicId: string) => `/clients/${publicId}/branches`,
  branchById: (publicId: string, branchId: string) =>
    `/clients/${publicId}/branches/${branchId}`,
  /** ADMIN. Sem body. 204. */
  deactivateBranch: (publicId: string, branchId: string) =>
    `/clients/${publicId}/branches/${branchId}/deactivate`,
  /** ADMIN. Sem body. 204. Idempotente: filial já ativa também devolve 204. */
  activateBranch: (publicId: string, branchId: string) =>
    `/clients/${publicId}/branches/${branchId}/activate`,
} as const;

/** Endpoints que exigem role ADMIN — o OpenAPI não expressa isso. */
export const ADMIN_ONLY = [
  'DELETE /clients/{publicId}',
  'POST /clients/{publicId}/branches/{branchId}/deactivate',
  'POST /clients/{publicId}/branches/{branchId}/activate',
] as const;

export function buildClientListQuery(filters: ClientFiltersParams): string {
  const params = new URLSearchParams();
  if (filters.name) params.set('name', filters.name);
  if (filters.status) params.set('status', filters.status);
  if (filters.tipo) params.set('tipo', filters.tipo);
  if (filters.documento) params.set('documento', filters.documento);
  filters.systems?.forEach((s) => params.append('systems', s));
  const qs = params.toString();
  return qs ? `/clients?${qs}` : '/clients';
}

// ---------------------------------------------------------------------------
// Regras de estado (para habilitar/desabilitar ações na UI)
// ---------------------------------------------------------------------------

/** Filial pode ser promovida a matriz: precisa estar ativa e não ser a matriz. */
export function canPromoteToMatriz(branch: BranchResponse, client: ClientResponse): boolean {
  return client.enabled && branch.enabled && !branch.isMatriz;
}

/** A matriz nunca pode ser inativada: promova outra filial antes. */
export function canDeactivateBranch(branch: BranchResponse, client: ClientResponse): boolean {
  return client.enabled && branch.enabled && !branch.isMatriz;
}

/** PF tem apenas a matriz: só reativa se não houver outra filial ativa. */
export function canActivateBranch(
  branch: BranchResponse,
  client: ClientResponse,
  allBranches: BranchResponse[],
): boolean {
  if (!client.enabled || branch.enabled) return false;
  if (client.tipo === ClientType.PF) {
    return !allBranches.some((b) => b.enabled);
  }
  return true;
}

/** Criar filial: só cliente PJ ativo. */
export function canCreateBranch(client: ClientResponse): boolean {
  return client.enabled && client.tipo === ClientType.PJ;
}

/** Editar dados da filial exige apenas cliente ativo (filial inativa é editável). */
export function canEditBranch(client: ClientResponse): boolean {
  return client.enabled;
}

/** O documento "do cliente" é o da matriz. */
export function documentoDoCliente(client: ClientResponse): string | null {
  return client.branches.find((b) => b.isMatriz)?.documento ?? null;
}
