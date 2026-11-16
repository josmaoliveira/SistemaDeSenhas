package br.com.sistemadesenhas.util;

/**
 * Validação de CPF e CNPJ pelos dígitos verificadores (o mesmo cálculo da Receita Federal).
 * Não consulta a Receita: só confere se o número é matematicamente válido.
 */
public final class Documentos {

    private Documentos() {
    }

    /** Remove tudo que não for número: "12.345.678/0001-95" vira "12345678000195". */
    public static String somenteNumeros(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    /**
     * @param cpf com ou sem pontuação
     * @return true se tiver 11 dígitos e os 2 dígitos verificadores estiverem corretos
     */
    public static boolean cpfValido(String cpf) {
        String n = somenteNumeros(cpf);
        // 11 dígitos e não pode ser tudo igual (111.111.111-11 passa no cálculo, mas é inválido)
        if (n.length() != 11 || n.chars().distinct().count() == 1) {
            return false;
        }
        int d1 = digitoCpf(n, 9, 10);
        int d2 = digitoCpf(n, 10, 11);
        return d1 == n.charAt(9) - '0' && d2 == n.charAt(10) - '0';
    }

    /** Calcula um dígito do CPF: soma dos dígitos * pesos decrescentes, depois resto de 11. */
    private static int digitoCpf(String n, int quantidade, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (n.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /**
     * @param cnpj com ou sem pontuação
     * @return true se tiver 14 dígitos e os 2 dígitos verificadores estiverem corretos
     */
    public static boolean cnpjValido(String cnpj) {
        String n = somenteNumeros(cnpj);
        if (n.length() != 14 || n.chars().distinct().count() == 1) {
            return false;
        }
        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int d1 = digitoCnpj(n, pesos1);
        int d2 = digitoCnpj(n, pesos2);
        return d1 == n.charAt(12) - '0' && d2 == n.charAt(13) - '0';
    }

    private static int digitoCnpj(String n, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (n.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
