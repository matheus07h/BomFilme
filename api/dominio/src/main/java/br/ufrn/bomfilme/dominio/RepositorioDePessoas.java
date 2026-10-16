package br.ufrn.bomfilme.dominio;

public interface RepositorioDePessoas {

    /** Resolve a pessoa pelo id do TMDB, criando-a quando o catálogo ainda não a conhece. */
    Pessoa buscarOuCriar(long tmdbId, String nome);
}
