package br.ufrn.bomfilme;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NovaRede(
        @NotBlank(message = "o nome da rede é obrigatório")
        @Size(max = 120, message = "o nome da rede deve ter no máximo 120 caracteres")
        String nome) {
}
