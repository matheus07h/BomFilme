package br.ufrn.bomfilme.dominio;

/** Uma participação no elenco como vem do catálogo externo. */
public record ParticipacaoDoCatalogo(Long idExterno, String nome, String personagem, Integer ordem) {
}
