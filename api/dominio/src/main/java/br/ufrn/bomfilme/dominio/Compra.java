package br.ufrn.bomfilme.dominio;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A compra de um assento numa sessão, com o valor cobrado em centavos de real.
 *
 * <p>{@code reservaId} é a reserva que segura o assento enquanto o pagamento não é decidido.
 */
public record Compra(Long id, Long sessaoId, Long assentoId, UUID reservaId, Long valorCentavos,
                     StatusDaCompra status, OffsetDateTime criadoEm) {

    public boolean pendente() {
        return status == StatusDaCompra.PENDENTE;
    }
}
