package br.ufrn.bomfilme.dtos.response;

public record MembroElencoResponse(
        Long id,
        String name,
        String character,
        Integer order
) {
}
