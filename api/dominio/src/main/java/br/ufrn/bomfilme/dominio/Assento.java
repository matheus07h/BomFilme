package br.ufrn.bomfilme.dominio;

import java.time.OffsetDateTime;

/** Um assento de uma sala, identificado pela fileira e pelo número. */
public record Assento(Long id, Long salaId, String fileira, Integer numero, OffsetDateTime criadoEm) {

    public static Assento novo(Long salaId, String fileira, Integer numero) {
        return new Assento(null, salaId, fileira, numero, null);
    }
}
