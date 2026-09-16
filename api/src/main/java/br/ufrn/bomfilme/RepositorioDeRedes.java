package br.ufrn.bomfilme;

import java.util.List;
import java.util.Optional;

public interface RepositorioDeRedes {

    List<RedeCinema> listar();

    Optional<RedeCinema> porId(long id);

    RedeCinema criar(String nome);
}
