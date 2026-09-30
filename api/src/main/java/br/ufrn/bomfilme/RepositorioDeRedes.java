package br.ufrn.bomfilme;

import java.util.List;
import java.util.Optional;

public interface RepositorioDeRedes {

    List<RedeCinema> listar(String nome, int pagina, int tamanho);

    Optional<RedeCinema> porId(long id);

    RedeCinema criar(String nome);

    Optional<RedeCinema> atualizar(long id, String nome);

    boolean remover(long id);
}
