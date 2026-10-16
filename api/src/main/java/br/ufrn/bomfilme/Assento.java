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

@Entity
@Table(name = "assento")
public class Assento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    public Sala sala;

    @Column(nullable = false, length = 2)
    public String fileira;

    @Column(nullable = false)
    public Integer numero;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    public OffsetDateTime criadoEm;

    public Assento() {
    }

    public Assento(Sala sala, String fileira, Integer numero) {
        this.sala = sala;
        this.fileira = fileira;
        this.numero = numero;
    }

    public Long getSalaId() {
        return sala == null ? null : sala.id;
    }
}
