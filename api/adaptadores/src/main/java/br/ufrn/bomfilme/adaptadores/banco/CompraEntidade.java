package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Compra;
import br.ufrn.bomfilme.dominio.StatusDaCompra;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "compra")
class CompraEntidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false)
    SessaoEntidade sessao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assento_id", nullable = false)
    AssentoEntidade assento;

    @Column(name = "reserva_id", nullable = false, updatable = false)
    UUID reservaId;

    @Column(name = "valor_centavos", nullable = false)
    Long valorCentavos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    StatusDaCompra status;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    OffsetDateTime criadoEm;

    CompraEntidade() {
    }

    CompraEntidade(SessaoEntidade sessao, AssentoEntidade assento, UUID reservaId, long valorCentavos) {
        this.sessao = sessao;
        this.assento = assento;
        this.reservaId = reservaId;
        this.valorCentavos = valorCentavos;
        this.status = StatusDaCompra.PENDENTE;
    }

    Compra paraDominio() {
        return new Compra(id, sessao.id, assento.id, reservaId, valorCentavos, status, criadoEm);
    }
}
