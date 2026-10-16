package br.ufrn.bomfilme.dominio;

import java.time.OffsetDateTime;

/** Uma sala de exibição, sempre vinculada a um cinema. */
public record Sala(Long id, Long cinemaId, String nome, OffsetDateTime criadoEm) {

    public static Sala nova(Long cinemaId, String nome) {
        return new Sala(null, cinemaId, nome, null);
    }
}
