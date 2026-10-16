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
@Table(name = "assento_ocupado")
class Ocupacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false)
    public Sessao sessao;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assento_id", nullable = false)
    public Assento assento;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    public OffsetDateTime criadoEm;

    public Ocupacao() {
    }

    public Ocupacao(Sessao sessao, Assento assento) {
        this.sessao = sessao;
        this.assento = assento;
    }
}

record AssentoDisponibilidade(Long id, String fileira, Integer numero, boolean disponivel) {
}
