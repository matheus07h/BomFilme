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
@Table(name = "cinema")
public class Cinema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rede_cinema_id", nullable = false)
    public RedeCinema rede;

    @Column(nullable = false, length = 120)
    public String nome;

    @Column(nullable = false, length = 120)
    public String cidade;

    @Column(nullable = false, length = 2)
    public String uf;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    public OffsetDateTime criadoEm;

    public Cinema() {
    }

    public Cinema(RedeCinema rede, String nome, String cidade, String uf) {
        this.rede = rede;
        this.nome = nome;
        this.cidade = cidade;
        this.uf = uf;
    }

    public Long getRedeId() {
        return rede == null ? null : rede.id;
    }
}
