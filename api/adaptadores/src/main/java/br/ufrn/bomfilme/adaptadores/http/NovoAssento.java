package br.ufrn.bomfilme.adaptadores.http;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record NovoAssento(
        @NotBlank(message = "a fileira é obrigatória")
        @Pattern(regexp = "[A-Za-z]{1,2}", message = "a fileira deve ter uma ou duas letras")
        String fileira,

        @NotNull(message = "o número do assento é obrigatório")
        @Min(value = 1, message = "o número do assento deve ser maior ou igual a um")
        @Max(value = 999, message = "o número do assento deve ser menor ou igual a 999")
        Integer numero) {
}
