package br.ufrn.bomfilme.dominio;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface RepositorioDeSessoes {

    List<Sessao> listar(FiltroDeSessoes filtro, int pagina, int tamanho);

    Optional<Sessao> porId(long id);

    Sessao criar(long salaId, long filmeId, OffsetDateTime inicio, OffsetDateTime fim);

    Optional<Sessao> atualizar(long id, long salaId, long filmeId, OffsetDateTime inicio, OffsetDateTime fim);

    boolean remover(long id);

    /** Indica se outra sessão da sala ocupa parte de [inicio, fim), ignorando a sessão informada. */
    boolean existeConflito(long salaId, OffsetDateTime inicio, OffsetDateTime fim, Long ignorarSessaoId);

    boolean possuiOcupacoes(long sessaoId);
}
