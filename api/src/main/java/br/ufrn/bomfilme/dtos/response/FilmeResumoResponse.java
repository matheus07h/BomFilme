package br.ufrn.bomfilme.dtos.response;

import br.ufrn.bomfilme.Filme;

import java.time.LocalDate;
import java.util.List;

public record FilmeResumoResponse(Long id, String titulo, String posterPath,
                                  LocalDate dataLancamento, Double notaMediaTmdb, List<String> generos) {
    public static FilmeResumoResponse from(Filme f) {
        return new FilmeResumoResponse(f.id, f.titulo, f.posterPath, f.dataLancamento,
                f.notaMediaTmdb, f.generos.stream().map(g -> g.nome).toList());
    }
}