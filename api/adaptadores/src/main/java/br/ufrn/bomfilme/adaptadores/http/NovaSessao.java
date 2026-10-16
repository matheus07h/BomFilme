package br.ufrn.bomfilme.adaptadores.http;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;

/** Dados de uma sessão; o fim é calculado pela duração do filme. */
public record NovaSessao(
        @NotNull(message = "a sala da sessão é obrigatória")
        @Positive(message = "o identificador da sala deve ser positivo")
        Long salaId,

        @NotNull(message = "o filme da sessão é obrigatório")
        @Positive(message = "o identificador do filme deve ser positivo")
        Long filmeId,

        @NotNull(message = "o início da sessão é obrigatório")
        @Future(message = "o início da sessão deve estar no futuro")
        OffsetDateTime inicio) {
}
