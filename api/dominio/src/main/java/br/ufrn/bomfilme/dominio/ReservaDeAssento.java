package br.ufrn.bomfilme.dominio;

import java.util.UUID;

/** Identifica a reserva que mantém um assento indisponível durante o pagamento. */
public record ReservaDeAssento(UUID id, Long sessaoId, Long assentoId) {
}
