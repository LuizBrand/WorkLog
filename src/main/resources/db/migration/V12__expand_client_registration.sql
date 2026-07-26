-- Novas colunas em clients
ALTER TABLE clients ADD COLUMN tipo VARCHAR(2);
ALTER TABLE clients ADD COLUMN nome_fantasia VARCHAR(100);
ALTER TABLE clients ADD COLUMN regime_tributario VARCHAR(30);

-- Backfill do tipo dos clients ja cadastrados e trava NOT NULL
UPDATE clients SET tipo = 'PJ' WHERE tipo IS NULL;
ALTER TABLE clients ALTER COLUMN tipo SET NOT NULL;

-- Tabela de Filiais
CREATE TABLE client_branches (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    public_id UUID DEFAULT gen_random_uuid() NOT NULL UNIQUE,
    client_id BIGINT NOT NULL,
    documento VARCHAR(20) UNIQUE,
    is_matriz BOOLEAN NOT NULL DEFAULT FALSE,
    apelido VARCHAR(100),
    inscricao_estadual VARCHAR(20),
    inscricao_municipal VARCHAR(20),
    cep VARCHAR(8),
    logradouro VARCHAR(150),
    numero VARCHAR(20),
    complemento VARCHAR(100),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    uf VARCHAR(2),
    is_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_branch_client FOREIGN KEY (client_id)
        REFERENCES clients(id) ON DELETE CASCADE
);

-- Tabela de Contatos
CREATE TABLE client_contacts (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    public_id UUID DEFAULT gen_random_uuid() NOT NULL UNIQUE,
    branch_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    valor VARCHAR(150) NOT NULL,
    descricao VARCHAR(100),
    principal BOOLEAN DEFAULT FALSE,
    is_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contact_branch FOREIGN KEY (branch_id)
        REFERENCES client_branches(id) ON DELETE CASCADE
);

-- Backfill: cada cliente existente ganha 1 matriz (documento vazio, editável)
INSERT INTO client_branches (client_id, is_matriz, apelido)
SELECT id, TRUE, 'Matriz' FROM clients;

