package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Assento;
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
class AssentoEntidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    SalaEntidade sala;

    @Column(nullable = false, length = 2)
    String fileira;

    @Column(nullable = false)
    Integer numero;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    OffsetDateTime criadoEm;

    AssentoEntidade() {
    }

    AssentoEntidade(SalaEntidade sala, String fileira, Integer numero) {
        this.sala = sala;
        this.fileira = fileira;
        this.numero = numero;
    }

    Assento paraDominio() {
        return new Assento(id, sala.id, fileira, numero, criadoEm);
    }
}
