package br.ufrn.bomfilme;

import br.ufrn.bomfilme.dtos.response.GeneroResponse;

import java.util.List;
import java.util.Optional;

public interface RepositorioDeGeneros {
    Optional<Genero> buscarPorTmdbId(long tmdbId);
    List<Genero> listarTodos();
    void persistir(Genero genero);
}