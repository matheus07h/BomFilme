package br.ufrn.bomfilme;

import br.ufrn.bomfilme.dtos.response.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class ServicoDeFilme {

    @Inject
    RepositorioDeFilmes repositorio;

    @Inject
    RepositorioDeGeneros repositorioGenero;

    @Inject
    RepositorioDePessoas repositorioPessoas;

    @Inject @RestClient ClienteTMDB clienteTMDB;

    // ServicoDeFilme — a conversão precisa acontecer AQUI, dentro da transação
    @Transactional
    public FilmeResponse importarBasico(Long tmdbId) {
        FilmeTMDBResponse dados = clienteTMDB.buscarPorId(tmdbId, null);
        Filme filme = buscarOuCriar(tmdbId);
        preencherDadosBasicos(filme, dados);
        repositorio.persistir(filme);
        return FilmeResponse.from(filme);
    }

    @Transactional
    public Filme importarCompleto(Long tmdbId) {
        FilmeTMDBResponse dados = clienteTMDB.buscarPorId(tmdbId, "credits");
        Filme filme = buscarOuCriar(tmdbId);
        preencherDadosBasicos(filme, dados);
        preencherElencoEDiretores(filme, dados.credits());
        repositorio.persistir(filme);
        return filme;
    }

    private Filme buscarOuCriar(Long tmdbId) {
        return repositorio.buscarPorTmdbId(tmdbId).orElseGet(Filme::new);
    }

    @Transactional
    public FilmeResponse buscarPorId(Long id) {
        Filme filme = repositorio.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Filme não encontrado!"));
        return FilmeResponse.from(filme);
    }

    @Transactional
    public List<FilmeResponse> buscarPorTitulo(String titulo, int pagina, int tamanho) {
        List<Filme> filmes = repositorio.buscarPorTitulo(titulo, pagina, tamanho);
        return filmes.stream().map(FilmeResponse::from).collect(Collectors.toList());
    }

    private void preencherDadosBasicos(Filme filme, FilmeTMDBResponse dados) {
        filme.tmdbId = dados.id();
        filme.titulo = dados.title();
        String tituloOriginal = dados.originalTitle();
        filme.tituloOriginal = tituloOriginal;
        filme.sinopse = dados.overview();
        filme.posterPath = dados.posterPath();
        filme.duracaoMinutos = dados.runtime();
        filme.dataLancamento = dados.releaseDate();
        filme.notaMediaTmdb = dados.voteAverage();

        filme.generos = dados.genres().stream()
                .map(this::buscarOuCriarGenero)
                .collect(Collectors.toSet());
    }

    private void preencherElencoEDiretores(Filme filme, CreditosTMDBResponse credits) {
        // 1. Garantir que as coleções não sejam nulas
        if (filme.elenco == null) {
            filme.elenco = new HashSet<>();
        }
        if (filme.diretores == null) {
            filme.diretores = new HashSet<>();
        }

        // 2. Atualizar Diretores (usando clear + addAll para manter a referência da coleção JPA)
        filme.diretores.clear();
        if (credits.crew() != null) {
            Set<Pessoa> novosDiretores = credits.crew().stream()
                    .filter(c -> "Director".equals(c.job()))
                    .map(c -> buscarOuCriarPessoa(c.id(), c.name()))
                    .collect(Collectors.toSet());
            filme.diretores.addAll(novosDiretores);
        }

        // 3. Atualizar Elenco
        filme.elenco.clear();

        if (credits.cast() != null) {
            credits.cast().stream()
                    .filter(membro -> membro.order() != null)
                    .sorted(Comparator.comparing(MembroElencoResponse::order))
                    .limit(6)
                    .forEach(membro -> {
                        Pessoa pessoa = buscarOuCriarPessoa(membro.id(), membro.name());

                        ElencoFilme elencoFilme = new ElencoFilme();
                        elencoFilme.filme = filme;
                        elencoFilme.pessoa = pessoa;
                        elencoFilme.personagem = membro.character();
                        elencoFilme.ordem = membro.order();

                        filme.elenco.add(elencoFilme);
                    });
        }
    }

    private Pessoa buscarOuCriarPessoa(Long tmdbId, String nome) {
        return repositorioPessoas.buscarPorTmdbId(tmdbId)
                .orElseGet(() -> {
                    Pessoa pessoa = new Pessoa();
                    pessoa.tmdbId = tmdbId;
                    pessoa.nome = nome;
                    repositorioPessoas.persistir(pessoa);
                    return pessoa;
                });
    }

    private Genero buscarOuCriarGenero(GeneroTMDBResponse dto) {
        return repositorioGenero.buscarPorTmdbId(dto.tmdbId())
                .orElseGet(() -> {
                    Genero genero = new Genero();
                    genero.tmdbId = dto.tmdbId();
                    genero.nome = dto.name();
                    repositorioGenero.persistir(genero);
                    return genero;
                });
    }

}
