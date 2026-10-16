package br.ufrn.bomfilme;

public record ResultadoSincronizacao(
        int pagina,
        int atualizados,
        int ignorados
) { }