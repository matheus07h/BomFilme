package br.ufrn.bomfilme.adaptadores.tmdb;

import java.util.List;

public record CreditosTMDBResponse(List<MembroElencoResponse> cast, List<EquipeResponse> crew) {
}
