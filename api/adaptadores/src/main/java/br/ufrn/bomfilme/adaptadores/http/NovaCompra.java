package br.ufrn.bomfilme.adaptadores.http;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record NovaCompra(
        @NotNull(message = "o assento é obrigatório")
        @Positive(message = "assentoId deve ser positivo")
        Long assentoId) {
}
