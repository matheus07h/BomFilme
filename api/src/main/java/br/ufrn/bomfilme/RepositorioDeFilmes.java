package br.ufrn.bomfilme;

import java.util.List;
import java.util.Optional;

public interface RepositorioDeFilmes{
    List<Filme> listar(int pagina, int tamanho);
    Optional<Filme> buscarPorId(long id);
    List<Filme> buscarPorTitulo(String titulo, int pagina, int tamanho);
    Optional<Filme> buscarPorTmdbId(long tmdbId);
    void persistir(Filme filme);
}
