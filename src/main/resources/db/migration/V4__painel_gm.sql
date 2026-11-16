-- =====================================================================
-- V4 - Painel do GM (administrador geral do sistema)
--
--  * O GM é um usuário SEM empresa (usuarios.empresa_id fica vazio).
--  * REGISTROS_LOGIN: histórico de acessos, consultado pelo GM.
--  * PAGAMENTOS: cada pagamento registrado pelo GM empurra o vencimento
--    da licença da empresa.
-- =====================================================================

CREATE TABLE registros_login (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT    NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    empresa_id BIGINT    REFERENCES empresas (id) ON DELETE CASCADE,  -- vazio quando é o GM
    data_hora  TIMESTAMP NOT NULL,
    ip         VARCHAR(64),
    provedor   VARCHAR(30)                                         -- password, google.com ou phone
);
CREATE INDEX ix_registros_login_data ON registros_login (data_hora DESC);

CREATE TABLE pagamentos (
    id                  BIGSERIAL PRIMARY KEY,
    empresa_id          BIGINT         NOT NULL REFERENCES empresas (id) ON DELETE CASCADE,
    valor               NUMERIC(10, 2) NOT NULL,
    data_pagamento      TIMESTAMP      NOT NULL,
    dias_adicionados    INTEGER        NOT NULL,
    forma               VARCHAR(20)    NOT NULL,                   -- MANUAL, PIX, BOLETO, CARTAO
    observacao          VARCHAR(255),
    vencimento_anterior DATE           NOT NULL,
    vencimento_novo     DATE           NOT NULL,
    registrado_por_id   BIGINT REFERENCES usuarios (id) ON DELETE SET NULL
);
CREATE INDEX ix_pagamentos_empresa ON pagamentos (empresa_id);
