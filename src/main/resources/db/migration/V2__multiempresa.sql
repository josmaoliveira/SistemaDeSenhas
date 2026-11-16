-- =====================================================================
-- V2 - Multiempresa + login pelo Firebase
--
-- O que muda:
--  * Nova tabela EMPRESAS. Tudo passa a pertencer a uma empresa (coluna empresa_id).
--  * A tabela ATENDENTES (com senha em texto puro) é substituída por USUARIOS:
--    a senha agora fica no Firebase, não no nosso banco.
--
-- ATENÇÃO: os dados de teste antigos (serviços, guichês, senhas, atendentes)
-- são APAGADOS, porque não pertencem a nenhuma empresa. Depois de atualizar,
-- cadastre sua empresa pela tela /cadastro.html.
--
-- "ON DELETE CASCADE": ao excluir uma empresa, todos os dados dela vão junto.
-- =====================================================================

DROP TABLE IF EXISTS atendimentos, senhas, guiches, prioridades, servicos, atendentes CASCADE;

-- ---------------------------------------------------------------------
-- EMPRESAS: cada cliente que usa o sistema
-- ---------------------------------------------------------------------
CREATE TABLE empresas (
    id                  BIGSERIAL PRIMARY KEY,
    razao_social        VARCHAR(150) NOT NULL,
    nome_fantasia       VARCHAR(150) NOT NULL,
    cnpj                VARCHAR(14)  NOT NULL UNIQUE,   -- só números
    cpf_responsavel     VARCHAR(11)  NOT NULL,          -- só números
    nome_responsavel    VARCHAR(150) NOT NULL,
    email               VARCHAR(150) NOT NULL,
    telefone            VARCHAR(20),
    telefone_verificado BOOLEAN      NOT NULL DEFAULT FALSE,
    slug                VARCHAR(60)  NOT NULL UNIQUE,   -- usado nos links do totem/painel
    data_cadastro       TIMESTAMP    NOT NULL
);

-- ---------------------------------------------------------------------
-- USUARIOS: quem faz login (administrador da empresa ou atendente).
-- O login em si (senha, Google, SMS) é do Firebase; aqui fica o perfil
-- e a empresa. "firebase_uid" é preenchido no primeiro login.
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT REFERENCES empresas (id) ON DELETE CASCADE,
    firebase_uid  VARCHAR(128) UNIQUE,
    nome          VARCHAR(150) NOT NULL,
    email         VARCHAR(150),
    telefone      VARCHAR(20),                     -- formato +5583999990000
    perfil        VARCHAR(20)  NOT NULL,           -- ADMIN_EMPRESA ou ATENDENTE
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP    NOT NULL,
    ultimo_login  TIMESTAMP
);
-- O mesmo e-mail/telefone não pode estar em dois usuários (e-mail ignorando maiúsculas)
CREATE UNIQUE INDEX ux_usuarios_email ON usuarios (LOWER(email)) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX ux_usuarios_telefone ON usuarios (telefone) WHERE telefone IS NOT NULL;
CREATE INDEX ix_usuarios_empresa ON usuarios (empresa_id);

-- ---------------------------------------------------------------------
-- Tabelas do atendimento, agora todas com empresa_id
-- ---------------------------------------------------------------------
CREATE TABLE servicos (
    id         BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT       NOT NULL REFERENCES empresas (id) ON DELETE CASCADE,
    nome       VARCHAR(100) NOT NULL,
    descricao  VARCHAR(255),
    sigla      VARCHAR(10)  NOT NULL
);
CREATE INDEX ix_servicos_empresa ON servicos (empresa_id);

CREATE TABLE prioridades (
    id         BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT       NOT NULL REFERENCES empresas (id) ON DELETE CASCADE,
    nome       VARCHAR(100) NOT NULL,
    peso       INTEGER      NOT NULL
);
CREATE INDEX ix_prioridades_empresa ON prioridades (empresa_id);

CREATE TABLE guiches (
    id         BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT       NOT NULL REFERENCES empresas (id) ON DELETE CASCADE,
    numero     INTEGER,
    nome       VARCHAR(100) NOT NULL,
    setor      VARCHAR(100),
    ocupado    BOOLEAN      NOT NULL DEFAULT FALSE,
    servico_id BIGINT       NOT NULL REFERENCES servicos (id) ON DELETE CASCADE
);
CREATE INDEX ix_guiches_empresa ON guiches (empresa_id);

CREATE TABLE senhas (
    id                BIGSERIAL PRIMARY KEY,
    empresa_id        BIGINT      NOT NULL REFERENCES empresas (id) ON DELETE CASCADE,
    codigo            VARCHAR(20) NOT NULL,
    data_hora_geracao TIMESTAMP   NOT NULL,
    status            VARCHAR(20) NOT NULL,        -- AGUARDANDO, CHAMADA, FINALIZADA
    servico_id        BIGINT      NOT NULL REFERENCES servicos (id) ON DELETE CASCADE,
    prioridade_id     BIGINT      NOT NULL REFERENCES prioridades (id) ON DELETE CASCADE
);
CREATE INDEX ix_senhas_fila ON senhas (empresa_id, servico_id, status);

CREATE TABLE atendimentos (
    id               BIGSERIAL PRIMARY KEY,
    empresa_id       BIGINT    NOT NULL REFERENCES empresas (id) ON DELETE CASCADE,
    data_hora_inicio TIMESTAMP NOT NULL,
    data_hora_fim    TIMESTAMP,
    senha_id         BIGINT    NOT NULL UNIQUE REFERENCES senhas (id) ON DELETE CASCADE,
    guiche_id        BIGINT    NOT NULL REFERENCES guiches (id) ON DELETE CASCADE,
    usuario_id       BIGINT    REFERENCES usuarios (id) ON DELETE SET NULL  -- quem atendeu
);
CREATE INDEX ix_atendimentos_empresa ON atendimentos (empresa_id, data_hora_fim);
