package br.com.sistemadesenhas.dto;

/**
 * Configuração que o navegador precisa para falar com o Firebase.
 * Esses valores NÃO são secretos (o próprio Firebase os coloca no código do site).
 */
public record ConfigPublicaResponse(boolean configurado, String apiKey, String authDomain,
                                    String projectId, String appId, boolean smsHabilitado) {
}
