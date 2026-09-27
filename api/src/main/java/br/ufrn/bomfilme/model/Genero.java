package br.ufrn.bomfilme.model;

import jakarta.persistence.*;

@Entity
@Table(name="genero")
public class Genero {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name="tmdb_id", nullable=false, unique=true)
    public Long tmdbId;

    @Column(nullable=false)
    public String nome;
}
