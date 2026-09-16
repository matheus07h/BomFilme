package br.ufrn.bomfilme;

import java.util.List;
import java.util.Optional;

public interface RepositorioDeCinemas {

    List<Cinema> listar();

    List<Cinema> porRede(long redeId);

    Optional<Cinema> porId(long id);

    Cinema criar(RedeCinema rede, String nome, String cidade, String uf);
}
