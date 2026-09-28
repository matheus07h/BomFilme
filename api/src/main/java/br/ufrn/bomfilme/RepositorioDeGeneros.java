package br.ufrn.bomfilme;

import java.util.Optional;

public interface RepositorioDeGeneros {
    Optional<Genero> buscarPorTmdbId(long tmdbId);
    void persistir(Genero genero);
}