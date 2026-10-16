package br.ufrn.bomfilme.dominio;

import java.util.List;

public interface RepositorioDeGeneros {

    List<Genero> listarTodos();

    /** Resolve o gênero pelo id do TMDB, criando-o quando o catálogo ainda não o conhece. */
    Genero buscarOuCriar(long tmdbId, String nome);
}
