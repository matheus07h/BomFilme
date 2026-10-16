package br.ufrn.bomfilme.dominio;

/**
 * O que a sincronização de populares fez: quantos filmes do catálogo tiveram a
 * popularidade atualizada e quantos do ranking externo foram ignorados por não
 * estarem no catálogo.
 */
public record ResultadoSincronizacao(int pagina, int atualizados, int ignorados) {
}
