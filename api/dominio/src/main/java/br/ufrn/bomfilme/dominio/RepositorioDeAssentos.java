package br.ufrn.bomfilme.dominio;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RepositorioDeAssentos {

    List<Assento> porSala(long salaId);

    Pagina<Assento> listarDaSala(long salaId, String fileira, int pagina, int tamanho);

    Optional<Assento> porId(long id);

    Assento criar(long salaId, String fileira, int numero);

    Optional<Assento> atualizar(long id, String fileira, int numero);

    boolean remover(long id);

    boolean possuiOcupacoes(long assentoId);

    Set<Long> ocupadosNaSessao(long sessaoId);

    boolean estaOcupado(long sessaoId, long assentoId);

    void ocupar(long sessaoId, long assentoId);

    void reservar(long sessaoId, long assentoId, UUID reservaId);

    boolean confirmarReserva(UUID reservaId);

    boolean liberarReserva(UUID reservaId);
}
