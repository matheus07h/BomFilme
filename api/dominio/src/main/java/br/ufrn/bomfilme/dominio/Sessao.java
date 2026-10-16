package br.ufrn.bomfilme.dominio;

import java.time.OffsetDateTime;

/**
 * Exibição de um filme numa sala, de {@code inicio} (inclusivo) a {@code fim} (exclusivo).
 *
 * <p>O fim vem da duração do filme no momento da programação e não é recalculado depois:
 * uma correção de duração no TMDB não deve mexer numa grade já publicada.
 */
public record Sessao(Long id, Long salaId, Long filmeId,
                     OffsetDateTime inicio, OffsetDateTime fim, OffsetDateTime criadoEm) {

    public static Sessao nova(Long salaId, Long filmeId, OffsetDateTime inicio, OffsetDateTime fim) {
        return new Sessao(null, salaId, filmeId, inicio, fim, null);
    }

    /** Duas sessões conflitam quando ocupam a mesma sala em intervalos que se tocam. */
    public boolean sobrepoe(OffsetDateTime outroInicio, OffsetDateTime outroFim) {
        return inicio.isBefore(outroFim) && fim.isAfter(outroInicio);
    }
}
