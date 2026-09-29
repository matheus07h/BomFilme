package br.ufrn.bomfilme;

import jakarta.persistence.*;

@Entity
@Table(name="elenco_filme")
public class ElencoFilme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name="filme_id", nullable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Filme filme;

    @ManyToOne
    @JoinColumn(name="pessoa_id", nullable = false)
    public Pessoa pessoa;

    @Column(nullable = false)
    public String personagem;

    @Column (name = "ordem_bilheteria", nullable=false)
    public Integer ordem;
}
