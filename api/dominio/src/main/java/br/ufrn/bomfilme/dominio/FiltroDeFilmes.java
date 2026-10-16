package br.ufrn.bomfilme.dominio;

public record FiltroDeFilmes(String titulo, Long generoId, Integer ano, String ordenarPor, String direcao) {
}
