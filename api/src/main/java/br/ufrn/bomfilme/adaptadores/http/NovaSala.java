package br.ufrn.bomfilme.adaptadores.http;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record NovaSala(
        @NotNull(message = "o cinema da sala é obrigatório")
        @Positive(message = "o identificador do cinema deve ser positivo")
        Long cinemaId,

        @NotBlank(message = "o nome da sala é obrigatório")
        @Size(max = 60, message = "o nome da sala deve ter no máximo 60 caracteres")
        String nome) {
}
