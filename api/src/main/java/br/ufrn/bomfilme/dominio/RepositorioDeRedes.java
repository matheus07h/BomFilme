package br.ufrn.bomfilme.dominio;

import java.util.Optional;

public interface RepositorioDeRedes {

    Pagina<RedeCinema> listar(String nome, int pagina, int tamanho);

    Optional<RedeCinema> porId(long id);

    RedeCinema criar(String nome);

    Optional<RedeCinema> atualizar(long id, String nome);

    boolean remover(long id);
}
