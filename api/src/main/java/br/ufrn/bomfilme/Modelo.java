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
@Table(name = "sala")
class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id", nullable = false)
    public Cinema cinema;

    @Column(nullable = false, length = 60)
    public String nome;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    public OffsetDateTime criadoEm;

    public Sala() {
    }

    public Sala(Cinema cinema, String nome) {
        this.cinema = cinema;
        this.nome = nome;
    }
}

@Entity
@Table(name = "assento")
class Assento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    public Sala sala;

    @Column(nullable = false, length = 2)
    public String fileira;

    @Column(nullable = false)
    public Integer numero;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    public OffsetDateTime criadoEm;

    public Assento() {
    }

    public Assento(Sala sala, String fileira, Integer numero) {
        this.sala = sala;
        this.fileira = fileira;
        this.numero = numero;
    }
}

@Entity
@Table(name = "sessao")
class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    public Sala sala;

    @Column(name = "filme_id", nullable = false)
    public Long filmeId;

    @Column(nullable = false)
    public OffsetDateTime inicio;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    public OffsetDateTime criadoEm;

    public Sessao() {
    }

    public Sessao(Sala sala, Long filmeId, OffsetDateTime inicio) {
        this.sala = sala;
        this.filmeId = filmeId;
        this.inicio = inicio;
    }
}

@Entity
@Table(name = "assento_ocupado")
class Ocupacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false)
    public Sessao sessao;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assento_id", nullable = false)
    public Assento assento;

    @Generated
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    public OffsetDateTime criadoEm;

    public Ocupacao() {
    }

    public Ocupacao(Sessao sessao, Assento assento) {
        this.sessao = sessao;
        this.assento = assento;
    }
}

record AssentoDisponibilidade(Long id, String fileira, Integer numero, boolean disponivel) {
}
