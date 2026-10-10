package br.ufrn.bomfilme;

import java.util.List;
import java.util.Optional;

public interface RepositorioDeGeneros {
    Optional<Genero> buscarPorTmdbId(long tmdbId);
    List<Genero> listarTodos();
    void persistir(Genero genero);
}
