package br.ufrn.bomfilme;

import br.ufrn.bomfilme.utils.FiltroDeFilmes;
import br.ufrn.bomfilme.utils.Pagina;

import java.util.List;
import java.util.Optional;

public interface RepositorioDeFilmes{
    Optional<Filme> buscarPorId(long id);
    Pagina<Filme> buscarPorFiltro(FiltroDeFilmes filtro, int pagina, int tamanho);
    Optional<Filme> buscarPorTmdbId(long tmdbId);
    void persistir(Filme filme);
    boolean deletar(long id);
}
