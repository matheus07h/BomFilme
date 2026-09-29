package br.ufrn.bomfilme.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ListarFilmesTMDBResponse(
        Integer page,
        List<ResumoFilmeTMDBResponse> results,
        @JsonProperty("total_pages") Integer totalPages,
        @JsonProperty("total_results") Integer totalResults
) {
}