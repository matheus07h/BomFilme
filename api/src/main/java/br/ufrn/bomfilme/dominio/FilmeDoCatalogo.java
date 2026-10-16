package br.ufrn.bomfilme.dominio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Dados de um filme como vêm do catálogo externo, antes de virarem um {@link Filme}.
 *
 * <p>Gêneros e pessoas aparecem aqui só com o id externo e o nome: é o caso de uso que
 * resolve cada um para o registro local, criando-o na primeira vez.
 *
 * <p>{@code creditos} é nulo quando a importação não pediu direção e elenco. Isso não é
 * o mesmo que pedir e não receber nada: na importação básica, o que já está gravado
 * precisa ser preservado.
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
        CreditosDoCatalogo creditos) {

    public FilmeDoCatalogo {
        generos = generos == null ? List.of() : List.copyOf(generos);
    }

    /** Vazio quando a importação não pediu os créditos. */
    public Optional<CreditosDoCatalogo> creditosPedidos() {
        return Optional.ofNullable(creditos);
    }
}
