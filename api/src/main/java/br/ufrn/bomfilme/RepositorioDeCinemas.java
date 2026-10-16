package br.ufrn.bomfilme;

import java.util.List;
import java.util.Optional;

public interface RepositorioDeCinemas {

    List<Cinema> listar(FiltroDeCinemas filtro, int pagina, int tamanho);

    Optional<Cinema> porId(long id);

    Cinema criar(RedeCinema rede, String nome, String cidade, String uf);

    Optional<Cinema> atualizar(long id, RedeCinema rede, String nome, String cidade, String uf);

    boolean remover(long id);

    boolean existePorRede(long redeId);

    boolean possuiSalas(long cinemaId);
}
