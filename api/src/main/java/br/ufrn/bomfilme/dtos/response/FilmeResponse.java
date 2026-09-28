package br.ufrn.bomfilme.dtos.response;

import br.ufrn.bomfilme.Filme;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record FilmeResponse(
        Long id,
        String titulo,
        String tituloOriginal,
        String sinopse,
        String posterPath,
        Integer duracaoMinutos,
        LocalDate dataLancamento,
        Double notaMediaTmdb,
        Double notaMediaSistema,
        List<String> generos,
        List<String> diretores,
        List<AtorFilmeResponse> elenco
) {
    public static FilmeResponse from(Filme filme) {
        return new FilmeResponse(
                filme.id,
                filme.titulo,
                filme.tituloOriginal,
                filme.sinopse,
                filme.posterPath,
                filme.duracaoMinutos,
                filme.dataLancamento,
                filme.notaMediaTmdb,
                filme.notaMediaSistema,
                filme.generos.stream().map(g -> g.nome).toList(),
                filme.diretores.stream().map(p -> p.nome).toList(),
                filme.elenco.stream()
                        .sorted(Comparator.comparing(e -> e.ordem))
                        .map(AtorFilmeResponse::from)
                        .toList()
        );
    }
}
