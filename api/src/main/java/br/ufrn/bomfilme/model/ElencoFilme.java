package br.ufrn.bomfilme.model;

import jakarta.persistence.*;

@Entity
@Table(name="elenco_filme")
public class ElencoFilme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name="filme_id", nullable=false)
    public Filme filme;

    @ManyToOne
    @JoinColumn(name="ator_id", nullable = false)
    public Pessoa ator;

    @Column(nullable = false)
    public String personagem;

    @Column (name = "ordem_bilheteria", nullable=false)
    public Integer ordem;
}
