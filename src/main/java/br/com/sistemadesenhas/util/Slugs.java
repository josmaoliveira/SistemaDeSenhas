package br.com.sistemadesenhas.util;

import java.text.Normalizer;

/** Gera identificadores amigáveis para URL: "Padaria São João" vira "padaria-sao-joao". */
public final class Slugs {

    private Slugs() {
    }

    public static String gerar(String texto) {
        String semAcento = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");                 // remove acentos
        String slug = semAcento.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")             // tudo que não é letra/número vira "-"
                .replaceAll("(^-+|-+$)", "");              // tira "-" do começo e do fim
        if (slug.length() > 50) {
            slug = slug.substring(0, 50).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? "empresa" : slug;
    }
}
