package br.ufrn.bomfilme.dominio;

import java.time.LocalDate;
import java.util.List;

/**
 * Dados de um filme como vêm do catálogo externo, antes de virarem um {@link Filme}.
 *
 * <p>Gêneros e pessoas aparecem aqui só com o id externo e o nome: é o caso de uso que
 * resolve cada um para o registro local, criando-o na primeira vez.
 */
public record FilmeDoCatalogo(
        Long idExterno,
        String titulo,
        String tituloOriginal,
        String sinopse,
        String posterPath,
        Integer duracaoMinutos,
        LocalDate dataLancamento,
        Double notaMedia,
        Double popularidade,
        List<ReferenciaExterna> generos,
        List<ReferenciaExterna> diretores,
        List<ParticipacaoDoCatalogo> elenco) {

    public FilmeDoCatalogo {
        generos = generos == null ? List.of() : List.copyOf(generos);
        diretores = diretores == null ? List.of() : List.copyOf(diretores);
        elenco = elenco == null ? List.of() : List.copyOf(elenco);
    }
}
