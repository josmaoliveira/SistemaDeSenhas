-- =====================================================================
-- V5 - Mensalidade das empresas (cobrada depois do período grátis)
--
--  * CONFIGURACOES: uma única linha com o valor mensal PADRÃO, que vale
--    para todas as empresas. O GM pode alterar (o preço pode subir).
--  * EMPRESAS.VALOR_MENSAL: valor próprio de uma empresa (ex.: desconto).
--    Vazio = a empresa paga o valor padrão.
-- =====================================================================

CREATE TABLE configuracoes (
    id                  INTEGER        PRIMARY KEY CHECK (id = 1),  -- sempre uma linha só
    valor_mensal_padrao NUMERIC(10, 2) NOT NULL
);

-- Valor inicial; o GM troca pelo painel (Resumo > Mensalidade padrão)
INSERT INTO configuracoes (id, valor_mensal_padrao) VALUES (1, 99.90);

ALTER TABLE empresas ADD COLUMN valor_mensal NUMERIC(10, 2);
