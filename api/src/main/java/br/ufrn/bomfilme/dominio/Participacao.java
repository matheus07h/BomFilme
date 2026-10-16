package br.ufrn.bomfilme.dominio;

/**
 * Participação de uma pessoa no elenco de um filme.
 *
 * <p>{@code ordem} é a posição no crédito de bilheteria vinda do TMDB: zero é o papel
 * principal. Serve para ordenar o elenco sem depender da ordem de gravação.
 */
public record Participacao(Pessoa pessoa, String personagem, Integer ordem) {
}
