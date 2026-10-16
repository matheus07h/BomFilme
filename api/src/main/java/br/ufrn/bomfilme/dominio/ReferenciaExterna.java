package br.ufrn.bomfilme.dominio;

/** Um gênero ou uma pessoa identificados apenas pelo id no catálogo externo. */
public record ReferenciaExterna(Long idExterno, String nome) {
}
