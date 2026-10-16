package br.ufrn.bomfilme.adaptadores.http;

import br.ufrn.bomfilme.dominio.Filme;
import br.ufrn.bomfilme.dominio.Genero;
import java.time.LocalDate;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Versão curta do filme, usada na listagem. */
@Schema(name = "FilmeResumo", description = "Dados reduzidos de um filme, para listagem")
public record FilmeResumoResponse(
        Long id,
        String titulo,
        String posterPath,
        LocalDate dataLancamento,
        Double notaMediaTmdb,
        List<String> generos) {

    public static FilmeResumoResponse de(Filme filme) {
        return new FilmeResumoResponse(
                filme.id(), filme.titulo(), filme.posterPath(), filme.dataLancamento(),
                filme.notaMediaTmdb(), filme.generos().stream().map(Genero::nome).toList());
    }
}
