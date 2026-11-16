-- =====================================================================
-- V1 - Estrutura inicial do banco (as mesmas tabelas que o sistema já usava)
--
-- Este arquivo é executado AUTOMATICAMENTE pelo Flyway quando o sistema sobe
-- com um banco vazio. O Flyway anota na tabela "flyway_schema_history" quais
-- arquivos já rodou, então cada arquivo roda uma única vez.
--
-- Se o banco JÁ TEM as tabelas (criadas antes pelo Hibernate), o Flyway não
-- roda este arquivo: só marca como feito (spring.flyway.baseline-on-migrate).
-- Assim ninguém perde dados.
--
-- REGRA DE OURO: nunca altere um arquivo que já rodou. Para mudar o banco,
-- crie um arquivo novo: V2__descricao.sql, V3__descricao.sql, ...
-- =====================================================================

-- Funcionários que fazem login (admin = acesso à tela de gestão)
CREATE TABLE atendentes (
    id       BIGSERIAL PRIMARY KEY,
    admin    BOOLEAN      NOT NULL,
    nome     VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL UNIQUE
);

-- Tipos de atendimento (Caixa, Atendimento Geral...). A sigla vira o prefixo da senha.
CREATE TABLE servicos (
    id        BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(255),
    nome      VARCHAR(255),
    sigla     VARCHAR(255)
);

-- Prioridades: peso MAIOR é chamado antes
CREATE TABLE prioridades (
    id   BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255),
    peso INTEGER
);

-- Postos de atendimento; cada guichê atende um serviço
CREATE TABLE guiches (
    id           BIGSERIAL PRIMARY KEY,
    nome         VARCHAR(255),
    numero       INTEGER,
    ocupado      BOOLEAN NOT NULL,
    setor        VARCHAR(255),
    atendente_id BIGINT REFERENCES atendentes (id),
    servico_id   BIGINT REFERENCES servicos (id)
);

-- Senhas retiradas no totem
CREATE TABLE senhas (
    id                BIGSERIAL PRIMARY KEY,
    codigo            VARCHAR(255),
    data_hora_geracao TIMESTAMP,
    status            VARCHAR(255) CHECK (status IN ('AGUARDANDO', 'CHAMADA', 'FINALIZADA')),
    prioridade_id     BIGINT REFERENCES prioridades (id),
    servico_id        BIGINT REFERENCES servicos (id)
);

-- Uma senha sendo atendida em um guichê (data_hora_fim vazia = em andamento)
CREATE TABLE atendimentos (
    id               BIGSERIAL PRIMARY KEY,
    data_hora_fim    TIMESTAMP,
    data_hora_inicio TIMESTAMP,
    guiche_id        BIGINT REFERENCES guiches (id),
    senha_id         BIGINT UNIQUE REFERENCES senhas (id)
);
