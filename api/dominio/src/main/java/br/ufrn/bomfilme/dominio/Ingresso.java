package br.ufrn.bomfilme.dominio;

import java.time.OffsetDateTime;
import java.util.UUID;

/** O ingresso de uma compra confirmada. O código é o que se apresenta na entrada. */
public record Ingresso(Long id, Long compraId, UUID codigo, OffsetDateTime emitidoEm) {
}
