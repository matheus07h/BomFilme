package br.ufrn.bomfilme.adaptadores.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeneroTMDBResponse(@JsonProperty("id") Long tmdbId, String name) {
}
