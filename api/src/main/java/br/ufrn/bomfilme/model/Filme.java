package br.ufrn.bomfilme.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "filme")
public class Filme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "tmdb_id", nullable = false, unique = true)
    public Long tmdbId;

    @Column(nullable = false)
    public String titulo;

    @Column(name="titulo_original", nullable = false)
    public String tituloOriginal;

    @Column(nullable = false)
    public String sinopse;

    @Column(name="poster_path")
    public String posterPath;

    @Column(name = "duracao_minutos")
    public Integer duracaoMinutos;

    @Column(name = "data_lancamento", nullable = false)
    public LocalDate dataLancamento;

    @Column(name ="nota_media_tmdb", nullable = false)
    public Double notaMediaTmdb;

    @Column(name="nota_media_sistema")
    public Double notaMediaSistema;

    @ManyToMany
    @JoinTable(name="filme_genero", joinColumns = @JoinColumn(name="filme_id"), inverseJoinColumns = @JoinColumn(name="genero_id"))
    public Set<Genero> generos = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "filme_diretor",
            joinColumns = @JoinColumn(name = "filme_id"),
            inverseJoinColumns = @JoinColumn(name = "pessoa_id")
    )
    public Set<Pessoa> diretores = new HashSet<>();

    @OneToMany(mappedBy = "filme", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<ElencoFilme> elenco = new HashSet<>();

    public Set<ElencoFilme> getElenco(){
        if(this.elenco == null){
            this.elenco = new HashSet<>();
        }
        return this.elenco;
    }
}
