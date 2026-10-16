package br.ufrn.bomfilme.adaptadores.banco;

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

/** Presença de uma linha aqui significa assento ocupado naquela sessão. */
@Entity
@Table(name = "assento_ocupado")
class OcupacaoEntidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false)
    SessaoEntidade sessao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assento_id", nullable = false)
    AssentoEntidade assento;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    OffsetDateTime criadoEm;

    OcupacaoEntidade() {
    }

    OcupacaoEntidade(SessaoEntidade sessao, AssentoEntidade assento) {
        this.sessao = sessao;
        this.assento = assento;
    }
}
