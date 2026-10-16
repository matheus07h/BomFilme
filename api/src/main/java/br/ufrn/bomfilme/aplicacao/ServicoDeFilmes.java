package br.ufrn.bomfilme.aplicacao;

import br.ufrn.bomfilme.dominio.CatalogoExternoDeFilmes;
import br.ufrn.bomfilme.dominio.ErroDeDominio;
import br.ufrn.bomfilme.dominio.Filme;
import br.ufrn.bomfilme.dominio.FilmeDoCatalogo;
import br.ufrn.bomfilme.dominio.FiltroDeFilmes;
import br.ufrn.bomfilme.dominio.Genero;
import br.ufrn.bomfilme.dominio.Pagina;
import br.ufrn.bomfilme.dominio.Participacao;
import br.ufrn.bomfilme.dominio.Pessoa;
import br.ufrn.bomfilme.dominio.PopularidadeExterna;
import br.ufrn.bomfilme.dominio.RepositorioDeFilmes;
import br.ufrn.bomfilme.dominio.RepositorioDeGeneros;
import br.ufrn.bomfilme.dominio.RepositorioDePessoas;
import br.ufrn.bomfilme.dominio.ResultadoSincronizacao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Comparator;
import java.util.List;

/** Casos de uso do catálogo de filmes. */
@ApplicationScoped
public class ServicoDeFilmes {

    @Inject
    RepositorioDeFilmes filmes;

    @Inject
    RepositorioDeGeneros generos;

    @Inject
    RepositorioDePessoas pessoas;

    @Inject
    CatalogoExternoDeFilmes catalogoExterno;

    /** Importa título, sinopse, duração, pôster e gêneros. */
    public Filme importarBasico(long idExterno) {
        return importar(catalogoExterno.buscar(idExterno, false));
    }

    /** Importa o básico mais o elenco e a direção. */
    public Filme importarCompleto(long idExterno) {
        return importar(catalogoExterno.buscar(idExterno, true));
    }

    public Filme porId(long id) {
        return filmes.porId(id)
                .orElseThrow(() -> new ErroDeDominio.NaoEncontrado("filme " + id + " não encontrado"));
    }

    public Pagina<Filme> listar(FiltroDeFilmes filtro, int pagina, int tamanho) {
        return filmes.listar(filtro, pagina, tamanho);
    }

    /** A FK de {@code sessao.filme_id} bloqueia a exclusão de filme com sessões vinculadas. */
    public void remover(long id) {
        if (!filmes.remover(id)) {
            throw new ErroDeDominio.NaoEncontrado("filme " + id + " não encontrado");
        }
    }

    /**
     * Atualiza a popularidade dos filmes que já estão no catálogo.
     *
     * <p>Não importa filme novo de propósito: cada rede escolhe manualmente o que entra
     * no catálogo, então um filme do ranking externo que ninguém adicionou é ignorado.
     */
    public ResultadoSincronizacao sincronizarPopulares(int pagina) {
        List<PopularidadeExterna> populares = catalogoExterno.populares(pagina);
        int atualizados = 0;
        for (PopularidadeExterna item : populares) {
            if (filmes.atualizarPopularidade(item.idExterno(), item.popularidade())) {
                atualizados++;
            }
        }
        return new ResultadoSincronizacao(pagina, atualizados, populares.size() - atualizados);
    }

    private Filme importar(FilmeDoCatalogo dados) {
        if (dados.dataLancamento() == null) {
            throw new ErroDeDominio.RegraViolada(
                    "Filme sem data de lançamento no TMDB, não entra no catálogo");
        }

        Long idLocal = filmes.porTmdbId(dados.idExterno()).map(Filme::id).orElse(null);
        Double notaDoSistema = filmes.porTmdbId(dados.idExterno())
                .map(Filme::notaMediaSistema)
                .orElse(null);

        return filmes.salvar(new Filme(
                idLocal,
                dados.idExterno(),
                dados.titulo(),
                dados.tituloOriginal(),
                dados.sinopse(),
                dados.posterPath(),
                dados.duracaoMinutos(),
                dados.dataLancamento(),
                dados.notaMedia(),
                notaDoSistema,
                dados.popularidade(),
                resolverGeneros(dados),
                resolverDiretores(dados),
                resolverElenco(dados)));
    }

    private List<Genero> resolverGeneros(FilmeDoCatalogo dados) {
        return dados.generos().stream()
                .map(referencia -> generos.buscarOuCriar(referencia.idExterno(), referencia.nome()))
                .toList();
    }

    private List<Pessoa> resolverDiretores(FilmeDoCatalogo dados) {
        return dados.diretores().stream()
                .map(referencia -> pessoas.buscarOuCriar(referencia.idExterno(), referencia.nome()))
                .toList();
    }

    /** Guarda só os primeiros nomes do crédito: o catálogo não precisa do elenco inteiro. */
    private List<Participacao> resolverElenco(FilmeDoCatalogo dados) {
        return dados.elenco().stream()
                .sorted(Comparator.comparing(participacao -> participacao.ordem()))
                .limit(Filme.LIMITE_DE_ELENCO)
                .map(participacao -> new Participacao(
                        pessoas.buscarOuCriar(participacao.idExterno(), participacao.nome()),
                        participacao.personagem(),
                        participacao.ordem()))
                .toList();
    }
}
