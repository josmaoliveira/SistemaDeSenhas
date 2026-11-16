-- =====================================================================
-- V6 - Presença nos guichês (para o relatório "Atendimento de hoje")
--
-- A tela do atendente avisa o servidor a cada minuto que continua aberta.
-- Guichê "ligado" = recebeu esse aviso nos últimos 2 minutos.
--  * ULTIMA_PRESENCA: quando chegou o último aviso
--  * USUARIO_PRESENTE_ID: quem está no guichê (vazio quando ninguém)
-- =====================================================================

ALTER TABLE guiches ADD COLUMN ultima_presenca TIMESTAMP;
ALTER TABLE guiches ADD COLUMN usuario_presente_id BIGINT REFERENCES usuarios (id) ON DELETE SET NULL;
