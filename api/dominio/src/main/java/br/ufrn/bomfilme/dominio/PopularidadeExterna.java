package br.ufrn.bomfilme.dominio;

/** A popularidade de um filme no catálogo externo, usada na sincronização. */
public record PopularidadeExterna(Long idExterno, Double popularidade) {
}
