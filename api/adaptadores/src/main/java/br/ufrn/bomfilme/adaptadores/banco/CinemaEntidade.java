package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Cinema;
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
@Table(name = "cinema")
class CinemaEntidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rede_cinema_id", nullable = false)
    RedeCinemaEntidade rede;

    @Column(nullable = false, length = 120)
    String nome;

    @Column(nullable = false, length = 120)
    String cidade;

    @Column(nullable = false, length = 2)
    String uf;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    OffsetDateTime criadoEm;

    CinemaEntidade() {
    }

    CinemaEntidade(RedeCinemaEntidade rede, String nome, String cidade, String uf) {
        this.rede = rede;
        this.nome = nome;
        this.cidade = cidade;
        this.uf = uf;
    }

    Cinema paraDominio() {
        return new Cinema(id, rede.id, nome, cidade, uf, criadoEm);
    }
}
