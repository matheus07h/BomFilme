package br.ufrn.bomfilme.dominio;

import java.util.List;
import java.util.Optional;

public interface RepositorioDeSalas {

    List<Sala> listar(FiltroDeSalas filtro, int pagina, int tamanho);

    Optional<Sala> porId(long id);

    Sala criar(long cinemaId, String nome);

    Optional<Sala> atualizar(long id, long cinemaId, String nome);

    boolean remover(long id);

    boolean possuiAssentos(long salaId);

    boolean possuiSessoes(long salaId);
}
