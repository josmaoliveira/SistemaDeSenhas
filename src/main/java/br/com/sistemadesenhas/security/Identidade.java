package br.com.sistemadesenhas.security;

/**
 * Quem fez o login, do jeito que o sistema precisa saber, SEM depender de qual serviço de login é usado
 * (hoje o Firebase). Os services usam só esta classe: trocar de serviço não muda nenhum deles.
 *
 * @param uid             id único da pessoa no serviço de login
 * @param email           e-mail em minúsculas (null quando entrou só por SMS)
 * @param emailVerificado true se o e-mail foi confirmado (link clicado, ou login pelo Google)
 * @param telefone        celular confirmado por SMS, ex.: +5583999990000 (ou null)
 * @param nome            nome informado pelo serviço de login, ex.: o nome da conta Google (ou null)
 * @param provedor        como entrou: password, google.com ou phone (gravado no histórico de logins)
 * @param emailPendente   true quando a forma de login exige e-mail confirmado e ele ainda não foi confirmado
 */
public record Identidade(String uid, String email, boolean emailVerificado, String telefone, String nome,
                         String provedor, boolean emailPendente) {
}
