package br.ufrn.bomfilme.dominio;

/** Pessoa do elenco ou da direção, espelhada do TMDB pelo {@code tmdbId}. */
public record Pessoa(Long id, Long tmdbId, String nome) {

    public static Pessoa nova(Long tmdbId, String nome) {
        return new Pessoa(null, tmdbId, nome);
    }
}
