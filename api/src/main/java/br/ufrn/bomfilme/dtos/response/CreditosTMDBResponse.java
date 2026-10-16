package br.ufrn.bomfilme.dtos.response;

import java.util.List;

public record CreditosTMDBResponse(
        List<MembroElencoResponse> cast,
        List<EquipeResponse> crew
) {
}
