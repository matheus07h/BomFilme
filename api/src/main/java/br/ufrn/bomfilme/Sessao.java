package br.ufrn.bomfilme;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.Generated;

/**
 * Exibição de um filme numa sala, de {@code inicio} (inclusivo) a {@code fim} (exclusivo).
 * O fim é calculado pela duração do filme, e o banco impede sessões sobrepostas na mesma sala.
 */
@Entity
@Table(name = "sessao")
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    public Sala sala;

    @Column(name = "filme_id", nullable = false)
    public Long filmeId;

    @Column(nullable = false)
    public OffsetDateTime inicio;

    @Column(nullable = false)
    public OffsetDateTime fim;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    public OffsetDateTime criadoEm;

    public Sessao() {
    }

    public Sessao(Sala sala, Long filmeId, OffsetDateTime inicio, OffsetDateTime fim) {
        this.sala = sala;
        this.filmeId = filmeId;
        this.inicio = inicio;
        this.fim = fim;
    }

    public Long getSalaId() {
        return sala == null ? null : sala.id;
    }
}
