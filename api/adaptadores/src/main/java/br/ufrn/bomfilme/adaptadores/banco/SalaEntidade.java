package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Sala;
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
@Table(name = "sala")
class SalaEntidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id", nullable = false)
    CinemaEntidade cinema;

    @Column(nullable = false, length = 60)
    String nome;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    OffsetDateTime criadoEm;

    SalaEntidade() {
    }

    SalaEntidade(CinemaEntidade cinema, String nome) {
        this.cinema = cinema;
        this.nome = nome;
    }

    Sala paraDominio() {
        return new Sala(id, cinema.id, nome, criadoEm);
    }
}
