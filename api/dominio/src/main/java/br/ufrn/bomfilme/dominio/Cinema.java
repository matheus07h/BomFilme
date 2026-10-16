package br.ufrn.bomfilme.dominio;

import java.time.OffsetDateTime;

/** Um cinema, sempre vinculado a uma rede. */
public record Cinema(Long id, Long redeId, String nome, String cidade, String uf, OffsetDateTime criadoEm) {

    public static Cinema novo(Long redeId, String nome, String cidade, String uf) {
        return new Cinema(null, redeId, nome, cidade, uf, null);
    }
}
