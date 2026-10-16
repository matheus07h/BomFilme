package br.ufrn.bomfilme.dominio;

import java.util.Optional;

public interface RepositorioDeFilmes {

    Optional<Filme> porId(long id);

    Optional<Filme> porTmdbId(long tmdbId);

    Pagina<Filme> listar(FiltroDeFilmes filtro, int pagina, int tamanho);

    /** Grava o filme, inserindo ou atualizando o registro de mesmo {@code tmdbId}. */
    Filme salvar(Filme filme);

    boolean remover(long id);

    /** Atualiza só a popularidade. Devolve falso quando o filme não está no catálogo. */
    boolean atualizarPopularidade(long tmdbId, Double popularidade);
}
