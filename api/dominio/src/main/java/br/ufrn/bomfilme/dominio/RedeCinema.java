package br.ufrn.bomfilme.dominio;

import java.time.OffsetDateTime;

/** Uma rede de cinemas. */
public record RedeCinema(Long id, String nome, OffsetDateTime criadoEm) {

    /** Rede ainda não gravada: o id e a data de criação vêm do banco. */
    public static RedeCinema nova(String nome) {
        return new RedeCinema(null, nome, null);
    }
}
