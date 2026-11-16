package br.com.sistemadesenhas.util;

/** Padroniza telefones no formato internacional usado pelo Firebase (ex.: +5583999990000). */
public final class Telefones {

    private Telefones() {
    }

    /**
     * Converte "(83) 99999-0000", "83999990000" ou "+55 83 99999-0000" para "+5583999990000".
     *
     * @return o telefone padronizado, ou null se vier vazio
     */
    public static String normalizar(String telefone) {
        String n = Documentos.somenteNumeros(telefone);
        if (n.isEmpty()) {
            return null;
        }
        // Já veio com o 55 do Brasil na frente (12 ou 13 dígitos)
        if (n.startsWith("55") && (n.length() == 12 || n.length() == 13)) {
            return "+" + n;
        }
        // DDD + número (10 ou 11 dígitos): coloca o +55
        if (n.length() == 10 || n.length() == 11) {
            return "+55" + n;
        }
        // Outro país ou formato: só coloca o +
        return "+" + n;
    }
}
