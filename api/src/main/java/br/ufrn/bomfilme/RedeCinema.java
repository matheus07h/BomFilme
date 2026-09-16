package br.ufrn.bomfilme;

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
public class RedeCinema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 120)
    public String nome;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    public OffsetDateTime criadoEm;

    public RedeCinema() {
    }

    public RedeCinema(String nome) {
        this.nome = nome;
    }
}
