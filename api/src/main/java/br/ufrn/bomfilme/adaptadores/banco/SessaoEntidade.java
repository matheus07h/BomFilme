package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Sessao;
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

/** A sobreposição de horários na mesma sala é impedida por restrição de exclusão (migração V6). */
@Entity
@Table(name = "sessao")
class SessaoEntidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    SalaEntidade sala;

    @Column(name = "filme_id", nullable = false)
    Long filmeId;

    @Column(nullable = false)
    OffsetDateTime inicio;

    @Column(nullable = false)
    OffsetDateTime fim;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    OffsetDateTime criadoEm;

    SessaoEntidade() {
    }

    SessaoEntidade(SalaEntidade sala, Long filmeId, OffsetDateTime inicio, OffsetDateTime fim) {
        this.sala = sala;
        this.filmeId = filmeId;
        this.inicio = inicio;
        this.fim = fim;
    }

    Sessao paraDominio() {
        return new Sessao(id, sala.id, filmeId, inicio, fim, criadoEm);
    }
}
