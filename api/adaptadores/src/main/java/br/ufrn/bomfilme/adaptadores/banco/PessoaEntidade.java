package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Pessoa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pessoa")
class PessoaEntidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "tmdb_id", nullable = false, unique = true)
    Long tmdbId;

    @Column(nullable = false)
    String nome;

    PessoaEntidade() {
    }

    PessoaEntidade(Long tmdbId, String nome) {
        this.tmdbId = tmdbId;
        this.nome = nome;
    }

    Pessoa paraDominio() {
        return new Pessoa(id, tmdbId, nome);
    }
}
