package br.ufrn.bomfilme.dominio;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioDeCompras {

    /**
     * Reserva o assento na sessão e cria a compra pendente, numa transação só.
     *
     * <p>Se o assento já estiver ocupado, a unicidade de (sessão, assento) no banco recusa
     * a reserva com {@link ErroDeDominio.Conflito} e nenhuma compra é criada.
     */
    Compra reservar(long sessaoId, long assentoId, UUID reservaId, long valorCentavos);

    Optional<Compra> porId(long id);

    /**
     * Confirma uma compra pendente e a reserva do assento, e emite o ingresso, numa
     * transação só.
     *
     * <p>Devolve vazio quando a compra não está pendente: uma compra recusada não ganha
     * ingresso, e uma já confirmada não ganha um segundo.
     */
    Optional<Ingresso> confirmar(long compraId);

    /** Marca uma compra pendente como recusada e libera a reserva do assento. */
    boolean recusar(long compraId);

    /** Marca uma compra pendente como expirada e libera a reserva do assento. */
    boolean expirar(long compraId);

    /** Compras ainda pendentes criadas antes do instante informado, da mais antiga à mais nova. */
    List<Compra> pendentesCriadasAntesDe(OffsetDateTime limite);

    Optional<Ingresso> ingressoDaCompra(long compraId);
}
