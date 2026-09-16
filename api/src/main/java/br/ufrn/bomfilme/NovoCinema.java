package br.ufrn.bomfilme;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record NovoCinema(
        @NotNull(message = "a rede do cinema é obrigatória")
        @Positive(message = "o identificador da rede deve ser positivo")
        Long redeId,

        @NotBlank(message = "o nome do cinema é obrigatório")
        @Size(max = 120, message = "o nome do cinema deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "a cidade é obrigatória")
        @Size(max = 120, message = "a cidade deve ter no máximo 120 caracteres")
        String cidade,

        @NotBlank(message = "a UF é obrigatória")
        @Pattern(regexp = "[A-Za-z]{2}", message = "a UF deve ter exatamente duas letras")
        String uf) {
}
