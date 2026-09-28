package br.ufrn.bomfilme.dtos.response;

import br.ufrn.bomfilme.ElencoFilme;

public record AtorFilmeResponse(
        String nomeAtor,
        String personagem
) {
    public static AtorFilmeResponse from(ElencoFilme elencoFilme) {
        return new AtorFilmeResponse(
                elencoFilme.pessoa.nome,
                elencoFilme.personagem
        );
    }
}