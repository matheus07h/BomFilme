package br.ufrn.bomfilme.dtos.response;

public record ResumoFilmeTMDBResponse(
        Long id,
        String title,
        Double popularity
) {
}
