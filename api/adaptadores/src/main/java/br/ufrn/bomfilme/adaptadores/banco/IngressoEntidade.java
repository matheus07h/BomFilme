package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Ingresso;
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
import java.util.UUID;
import org.hibernate.annotations.Generated;

/** A unicidade de {@code compra_id} no banco garante um ingresso por compra. */
@Entity
@Table(name = "ingresso")
class IngressoEntidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    CompraEntidade compra;

    @Column(nullable = false, updatable = false)
    UUID codigo;

    @Generated
    @Column(name = "emitido_em", nullable = false, insertable = false, updatable = false)
    OffsetDateTime emitidoEm;

    IngressoEntidade() {
    }

    IngressoEntidade(CompraEntidade compra) {
        this.compra = compra;
        this.codigo = UUID.randomUUID();
    }

    Ingresso paraDominio() {
        return new Ingresso(id, compra.id, codigo, emitidoEm);
    }
}
