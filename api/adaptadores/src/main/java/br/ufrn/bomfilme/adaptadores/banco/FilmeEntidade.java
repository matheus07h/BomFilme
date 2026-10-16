package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Filme;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "filme")
class FilmeEntidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "tmdb_id", nullable = false, unique = true)
    Long tmdbId;

    @Column(nullable = false)
    String titulo;

    @Column(name = "titulo_original", nullable = false)
    String tituloOriginal;

    @Column(nullable = false)
    String sinopse;

    @Column(name = "poster_path")
    String posterPath;

    @Column(name = "duracao_minutos")
    Integer duracaoMinutos;

    @Column(name = "data_lancamento", nullable = false)
    LocalDate dataLancamento;

    @Column(name = "nota_media_tmdb", nullable = false)
    Double notaMediaTmdb;

    @Column(name = "nota_media_sistema")
    Double notaMediaSistema;

    @Column
    Double popularidade;

    /** O batch evita uma consulta de gêneros por filme ao montar uma página da listagem. */
    @BatchSize(size = 50)
    @ManyToMany
    @JoinTable(name = "filme_genero",
            joinColumns = @JoinColumn(name = "filme_id"),
            inverseJoinColumns = @JoinColumn(name = "genero_id"))
    Set<GeneroEntidade> generos = new HashSet<>();

    @BatchSize(size = 50)
    @ManyToMany
    @JoinTable(name = "filme_diretor",
            joinColumns = @JoinColumn(name = "filme_id"),
            inverseJoinColumns = @JoinColumn(name = "pessoa_id"))
    Set<PessoaEntidade> diretores = new HashSet<>();

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "filme", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<ElencoFilmeEntidade> elenco = new HashSet<>();

    Filme paraDominio() {
        return new Filme(id, tmdbId, titulo, tituloOriginal, sinopse, posterPath, duracaoMinutos,
                dataLancamento, notaMediaTmdb, notaMediaSistema, popularidade,
                generos.stream().map(GeneroEntidade::paraDominio).toList(),
                diretores.stream().map(PessoaEntidade::paraDominio).toList(),
                elenco.stream().map(ElencoFilmeEntidade::paraDominio).toList());
    }
}
