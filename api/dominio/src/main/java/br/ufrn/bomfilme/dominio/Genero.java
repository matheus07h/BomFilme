package br.ufrn.bomfilme.dominio;

/** Gênero de filme, espelhado do TMDB pelo {@code tmdbId}. */
public record Genero(Long id, Long tmdbId, String nome) {

    public static Genero novo(Long tmdbId, String nome) {
        return new Genero(null, tmdbId, nome);
    }
}
