package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.RedeCinema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "rede_cinema")
class RedeCinemaEntidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 120)
    String nome;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    OffsetDateTime criadoEm;

    RedeCinemaEntidade() {
    }

    RedeCinemaEntidade(String nome) {
        this.nome = nome;
    }

    RedeCinema paraDominio() {
        return new RedeCinema(id, nome, criadoEm);
    }
}
