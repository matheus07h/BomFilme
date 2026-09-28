package br.ufrn.bomfilme;

import java.util.Optional;

public interface RepositorioDePessoas {
    Optional<Pessoa> buscarPorTmdbId(Long tmdbId);
    void persistir(Pessoa pessoa);
}
