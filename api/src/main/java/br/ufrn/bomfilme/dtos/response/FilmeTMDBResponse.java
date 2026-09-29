package br.ufrn.bomfilme.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

public record FilmeTMDBResponse (
        Long id,
        String title,

        @JsonProperty("original_title")
        String originalTitle,

        String overview,
        Integer runtime,

        @JsonProperty("poster_path")
        String posterPath,

        @JsonProperty("release_date")
        LocalDate releaseDate,

        @JsonProperty("vote_average")
        Double voteAverage,

        List<GeneroTMDBResponse> genres,
        CreditosTMDBResponse credits,
        Double popularity
) {}