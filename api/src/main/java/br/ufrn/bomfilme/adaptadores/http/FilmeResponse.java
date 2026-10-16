package br.ufrn.bomfilme.adaptadores.http;

import br.ufrn.bomfilme.dominio.Filme;
import br.ufrn.bomfilme.dominio.Genero;
import br.ufrn.bomfilme.dominio.Pessoa;
import java.time.LocalDate;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Um filme com gêneros, direção e elenco. */
@Schema(name = "Filme", description = "Um filme do catálogo, com gêneros, direção e elenco")
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
        List<AtorFilmeResponse> elenco) {

    public static FilmeResponse de(Filme filme) {
        return new FilmeResponse(
                filme.id(),
                filme.titulo(),
                filme.tituloOriginal(),
                filme.sinopse(),
                filme.posterPath(),
                filme.duracaoMinutos(),
                filme.dataLancamento(),
                filme.notaMediaTmdb(),
                filme.notaMediaSistema(),
                filme.generos().stream().map(Genero::nome).toList(),
                filme.diretores().stream().map(Pessoa::nome).toList(),
                filme.elencoPorOrdem().stream().map(AtorFilmeResponse::de).toList());
    }
}
