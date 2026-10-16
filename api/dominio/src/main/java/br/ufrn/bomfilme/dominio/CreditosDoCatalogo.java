package br.ufrn.bomfilme.dominio;

import java.util.List;

/**
 * Direção e elenco de um filme no catálogo externo.
 *
 * <p>Existe para separar "não pedi os créditos" de "pedi e vieram vazios". O primeiro
 * caso é um {@link FilmeDoCatalogo} com {@code creditos} nulo, e preserva o que já está
 * gravado; o segundo é este registro com listas vazias, e sobrescreve.
 */
public record CreditosDoCatalogo(List<ReferenciaExterna> diretores, List<ParticipacaoDoCatalogo> elenco) {

    public CreditosDoCatalogo {
        diretores = diretores == null ? List.of() : List.copyOf(diretores);
        elenco = elenco == null ? List.of() : List.copyOf(elenco);
    }
}
