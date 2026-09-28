package br.ufrn.bomfilme.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeneroTMDBResponse(
        @JsonProperty("id") Long tmdbId,
        String name
) { }