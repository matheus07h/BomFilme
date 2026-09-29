package br.ufrn.bomfilme.dtos.response;

import br.ufrn.bomfilme.Genero;

public record GeneroResponse(Long id, String nome) {
    public static GeneroResponse from(Genero genero) {
        return new GeneroResponse(genero.id, genero.nome);
    }
}
