package br.ufrn.bomfilme;

import java.util.Locale;

final class Busca {

    private Busca() {
    }

    static boolean vazia(String texto) {
        return texto == null || texto.isBlank();
    }

    static String contendo(String texto) {
        String escapado = texto.strip()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
