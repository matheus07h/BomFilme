package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Genero;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "genero")
class GeneroEntidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "tmdb_id", nullable = false, unique = true)
    Long tmdbId;

    @Column(nullable = false)
    String nome;

    GeneroEntidade() {
    }

    GeneroEntidade(Long tmdbId, String nome) {
        this.tmdbId = tmdbId;
        this.nome = nome;
    }

    Genero paraDominio() {
        return new Genero(id, tmdbId, nome);
    }
}
