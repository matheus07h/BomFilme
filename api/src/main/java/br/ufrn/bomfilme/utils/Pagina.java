package br.ufrn.bomfilme.utils;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(List<T> itens, int pagina, int tamanho, long total, int totalPaginas) {

    public static <T> Pagina<T> de(List<T> itens, int pagina, int tamanho, long total) {
        return new Pagina<>(itens, pagina, tamanho, total, (int) Math.ceil((double) total / tamanho));
    }

    public <R> Pagina<R> mapear(Function<T, R> conversor) {
        return new Pagina<>(itens.stream().map(conversor).toList(), pagina, tamanho, total, totalPaginas);
    }
}

