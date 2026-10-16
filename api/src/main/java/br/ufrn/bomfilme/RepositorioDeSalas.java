package br.ufrn.bomfilme;

import java.util.List;
import java.util.Optional;

public interface RepositorioDeSalas {

    List<Sala> listar(FiltroDeSalas filtro, int pagina, int tamanho);

    Optional<Sala> porId(long id);

    Sala criar(Cinema cinema, String nome);

    Optional<Sala> atualizar(long id, Cinema cinema, String nome);

    boolean remover(long id);

    boolean possuiAssentos(long salaId);

    boolean possuiSessoes(long salaId);
}
