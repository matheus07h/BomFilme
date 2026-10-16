package br.ufrn.bomfilme.dominio;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Um filme do catálogo, importado do TMDB.
 *
 * <p>{@code dataLancamento} é obrigatória: o sistema é de cinema, e filme sem data de
 * lançamento não entra no catálogo. {@code notaMediaSistema} é a nota dos nossos
 * usuários e nasce vazia; {@code notaMediaTmdb} e {@code popularidade} vêm do TMDB.
 */
public record Filme(
        Long id,
        Long tmdbId,
        String titulo,
        String tituloOriginal,
        String sinopse,
        String posterPath,
        Integer duracaoMinutos,
        LocalDate dataLancamento,
        Double notaMediaTmdb,
        Double notaMediaSistema,
        Double popularidade,
        List<Genero> generos,
        List<Pessoa> diretores,
        List<Participacao> elenco) {

    /** Quantos nomes do elenco o catálogo guarda, em ordem de crédito. */
    public static final int LIMITE_DE_ELENCO = 6;

    public Filme {
        generos = generos == null ? List.of() : List.copyOf(generos);
        diretores = diretores == null ? List.of() : List.copyOf(diretores);
        elenco = elenco == null ? List.of() : List.copyOf(elenco);
    }

    /** O elenco em ordem de crédito, do papel principal para os demais. */
    public List<Participacao> elencoPorOrdem() {
        return elenco.stream()
                .sorted(Comparator.comparing(Participacao::ordem,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Uma sessão só pode ser programada quando o filme tem duração conhecida. */
    public boolean temDuracaoConhecida() {
        return duracaoMinutos != null && duracaoMinutos > 0;
    }
}
