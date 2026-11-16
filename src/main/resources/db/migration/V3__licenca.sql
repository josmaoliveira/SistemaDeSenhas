-- =====================================================================
-- V3 - Licença das empresas
--
-- Cada empresa ganha:
--  * serial            : código XXXX-XXXX-XXXX-XXXX calculado a partir do CNPJ.
--                        Empresas que já existiam ficam sem serial aqui; o sistema
--                        calcula e preenche ao subir (veja PreenchimentoDeSeriais.java),
--                        porque o cálculo usa um segredo que só o servidor conhece.
--  * versao_serial     : aumenta quando um serial novo é gerado para o mesmo CNPJ.
--  * status            : ATIVA ou BLOQUEADA (bloqueio manual, usado pelo GM na etapa 4).
--  * em_periodo_gratis : TRUE até o primeiro pagamento.
--  * data_vencimento   : último dia de uso liberado.
-- =====================================================================

ALTER TABLE empresas
    ADD COLUMN serial            VARCHAR(19),
    ADD COLUMN versao_serial     INTEGER     NOT NULL DEFAULT 1,
    ADD COLUMN status            VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
    ADD COLUMN em_periodo_gratis BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN data_vencimento   DATE;

-- Empresas já cadastradas ganham os 90 dias grátis contados a partir da data de cadastro
UPDATE empresas SET data_vencimento = CAST(data_cadastro AS DATE) + 90;

ALTER TABLE empresas ALTER COLUMN data_vencimento SET NOT NULL;
