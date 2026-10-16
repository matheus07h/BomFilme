package br.ufrn.bomfilme.dominio;

import java.util.Locale;

/** Normaliza o texto dos filtros de busca. */
public final class Busca {

    private Busca() {
    }

    public static boolean vazia(String texto) {
        return texto == null || texto.isBlank();
    }

    /**
     * Monta o padrão de um {@code like} que casa em qualquer posição.
     *
     * <p>Escapa {@code \}, {@code %} e {@code _} para que um filtro com esses caracteres
     * procure o literal em vez de virar curinga.
     */
    public static String contendo(String texto) {
        String escapado = texto.strip()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
