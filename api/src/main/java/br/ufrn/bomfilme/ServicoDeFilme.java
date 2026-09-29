package br.ufrn.bomfilme;

import br.ufrn.bomfilme.dtos.response.*;
import br.ufrn.bomfilme.utils.FiltroDeFilmes;
import br.ufrn.bomfilme.utils.Pagina;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ApplicationScoped
public class ServicoDeFilme {

    @Inject
    RepositorioDeFilmes repositorio;

    @Inject
    RepositorioDeGeneros repositorioGenero;

    @Inject
    RepositorioDePessoas repositorioPessoas;

    @Inject
    @RestClient
    ClienteTMDB clienteTMDB;

    @Transactional
    public FilmeResponse importarBasico(Long tmdbId) {
        FilmeTMDBResponse dados = chamarTmdb(() -> clienteTMDB.buscarPorId(tmdbId, null));
        Filme filme = buscarOuCriar(tmdbId);
        preencherDadosBasicos(filme, dados);
        repositorio.persistir(filme);
        return FilmeResponse.from(filme);
    }

    @Transactional
    public FilmeResponse importarCompleto(Long tmdbId) {
        FilmeTMDBResponse dados = chamarTmdb(() -> clienteTMDB.buscarPorId(tmdbId, "credits"));
        Filme filme = buscarOuCriar(tmdbId);
        preencherDadosBasicos(filme, dados);
        preencherElencoEDiretores(filme, dados.credits());
        repositorio.persistir(filme);
        return FilmeResponse.from(filme);
    }

    @Transactional
    public FilmeResponse buscarPorId(Long id) {
        Filme filme = repositorio.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Filme não encontrado!"));
        return FilmeResponse.from(filme);
    }

    @Transactional
    public Pagina<FilmeResumoResponse> listar(FiltroDeFilmes filtro, int pagina, int tamanho) {
        return repositorio.buscarPorFiltro(filtro, pagina, tamanho).mapear(FilmeResumoResponse::from);
    }

    public ResultadoSincronizacao sincronizarPopulares(int pagina) {
        ListarFilmesTMDBResponse populares = chamarTmdb(() -> clienteTMDB.listarPopulares(pagina));
        int atualizados = 0;
        int ignorados = 0;
        for (var item : populares.results()) {
            boolean existia = atualizarPopularidadeSeExistir(item.id(), item.popularity());
            if (existia) atualizados++; else ignorados++;
        }
        return new ResultadoSincronizacao(pagina, atualizados, ignorados);
    }

    @Transactional
    public boolean atualizarPopularidadeSeExistir(Long tmdbId, Double popularidade) {
        return repositorio.buscarPorTmdbId(tmdbId)
                .map(filme -> { filme.popularidade = popularidade; return true; })
                .orElse(false);
    }

    private <T> T chamarTmdb(Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                throw new NotFoundException("Não encontrado no TMDB");
            }
            throw new WebApplicationException("Falha ao consultar o TMDB", Response.Status.BAD_GATEWAY);
        } catch (ProcessingException e) {
            throw new WebApplicationException("TMDB indisponível", Response.Status.BAD_GATEWAY);
        }
    }

    private Filme buscarOuCriar(Long tmdbId) {
        return repositorio.buscarPorTmdbId(tmdbId).orElseGet(Filme::new);
    }


    // TODO: quando Sessao existir, trocar por soft delete (coluna "ativo") ou bloquear
    //  a exclusão se houver sessões vinculadas — hoje é hard delete, seguro só porque
    //  Filme ainda não tem nada dependendo dele além das tabelas de associação (cascade).
    @Transactional
    public void deletar(long id) {
        boolean existia = repositorio.deletar(id);
        if (!existia) {
            throw new NotFoundException("Filme não encontrado");
        }
    }

    private void preencherDadosBasicos(Filme filme, FilmeTMDBResponse dados) {
        if (dados.releaseDate() == null) {
            throw new WebApplicationException(
                    "Filme sem data de lançamento no TMDB, não entra no catálogo", 422);
        }

        filme.tmdbId = dados.id();
        filme.titulo = dados.title();
        filme.tituloOriginal = dados.originalTitle();
        filme.sinopse = dados.overview();
        filme.posterPath = dados.posterPath();
        filme.duracaoMinutos = dados.runtime();
        filme.dataLancamento = dados.releaseDate();
        filme.notaMediaTmdb = dados.voteAverage();
        filme.popularidade = dados.popularity();


        Set<Genero> generos = dados.genres() == null ? Set.of() : dados.genres().stream()
                .map(this::buscarOuCriarGenero)
                .collect(Collectors.toSet());
        filme.generos.clear();
        filme.generos.addAll(generos);
    }


    private void preencherElencoEDiretores(Filme filme, CreditosTMDBResponse credits) {
        if (credits == null) {
            return;
        }

        filme.diretores.clear();
        if (credits.crew() != null) {
            Set<Pessoa> novosDiretores = credits.crew().stream()
                    .filter(c -> "Director".equals(c.job()))
                    .map(c -> buscarOuCriarPessoa(c.id(), c.name()))
                    .collect(Collectors.toSet());
            filme.diretores.addAll(novosDiretores);
        }

        filme.elenco.clear();
        if (credits.cast() != null) {
            credits.cast().stream()
                    .filter(membro -> membro.order() != null)
                    .sorted(Comparator.comparing(MembroElencoResponse::order))
                    .limit(6)
                    .forEach(membro -> {
                        ElencoFilme elencoFilme = new ElencoFilme();
                        elencoFilme.filme = filme;
                        elencoFilme.pessoa = buscarOuCriarPessoa(membro.id(), membro.name());
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