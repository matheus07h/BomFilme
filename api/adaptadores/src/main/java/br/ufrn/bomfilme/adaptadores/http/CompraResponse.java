package br.ufrn.bomfilme.adaptadores.http;

import br.ufrn.bomfilme.dominio.Compra;
import br.ufrn.bomfilme.dominio.Ingresso;
import br.ufrn.bomfilme.dominio.StatusDaCompra;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * A compra como o cliente a vê. O id da reserva fica de fora: é detalhe interno de como o
 * assento é preso, e o cliente não faz nada com ele.
 */
public record CompraResponse(
        Long id,
        Long sessaoId,
        Long assentoId,
        Long valorCentavos,
        @Schema(description = "PENDENTE enquanto o pagamento não foi decidido; só CONFIRMADA tem ingresso")
        StatusDaCompra status,
        OffsetDateTime criadoEm,
        @Schema(description = "Presente só quando a compra está confirmada", nullable = true)
        IngressoResponse ingresso) {

    public record IngressoResponse(UUID codigo, OffsetDateTime emitidoEm) {
    }

    static CompraResponse de(Compra compra, Optional<Ingresso> ingresso) {
        return new CompraResponse(
                compra.id(), compra.sessaoId(), compra.assentoId(), compra.valorCentavos(),
                compra.status(), compra.criadoEm(),
                ingresso.map(i -> new IngressoResponse(i.codigo(), i.emitidoEm())).orElse(null));
    }
}
