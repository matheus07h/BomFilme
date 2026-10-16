package br.ufrn.bomfilme.dominio;

/** Um assento da sala e se ele está livre numa sessão específica. */
public record AssentoDisponibilidade(Long id, String fileira, Integer numero, boolean disponivel) {
}
