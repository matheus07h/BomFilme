package br.ufrn.bomfilme.aplicacao;

import br.ufrn.bomfilme.dominio.CatalogoExternoDeFilmes;
import br.ufrn.bomfilme.dominio.CreditosDoCatalogo;
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
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    /**
     * Grava o filme importado.
     *
     * <p>É uma transação só: resolver um gênero ou uma pessoa cria registro, e sem a
     * fronteira aqui esses registros ficariam gravados mesmo que a gravação do filme
     * falhasse depois.
     */
    @Transactional
    Filme importar(FilmeDoCatalogo dados) {
        if (dados.dataLancamento() == null) {
            throw new ErroDeDominio.RegraViolada(
                    "Filme sem data de lançamento no TMDB, não entra no catálogo");
        }

        Optional<Filme> existente = filmes.porTmdbId(dados.idExterno());

        return filmes.salvar(new Filme(
                existente.map(Filme::id).orElse(null),
                dados.idExterno(),
                dados.titulo(),
                dados.tituloOriginal(),
                dados.sinopse(),
                dados.posterPath(),
                dados.duracaoMinutos(),
                dados.dataLancamento(),
                dados.notaMedia(),
                // A nota do sistema é dos nossos usuários: o catálogo externo não a dita.
                existente.map(Filme::notaMediaSistema).orElse(null),
                dados.popularidade(),
                resolverGeneros(dados),
                diretoresPara(dados, existente),
                elencoPara(dados, existente)));
    }

    /**
     * Direção a gravar: a que veio, quando os créditos foram pedidos; a que já está
     * gravada, quando não foram.
     *
     * <p>A importação básica não pede créditos, e não pedir não é o mesmo que receber
     * uma lista vazia — sem essa distinção, importar o básico de um filme já completo
     * apagaria a direção e o elenco dele.
     */
    private List<Pessoa> diretoresPara(FilmeDoCatalogo dados, Optional<Filme> existente) {
        return dados.creditosPedidos()
                .map(this::resolverDiretores)
                .orElseGet(() -> existente.map(Filme::diretores).orElseGet(List::of));
    }

    private List<Participacao> elencoPara(FilmeDoCatalogo dados, Optional<Filme> existente) {
        return dados.creditosPedidos()
                .map(this::resolverElenco)
                .orElseGet(() -> existente.map(Filme::elenco).orElseGet(List::of));
    }

    private List<Genero> resolverGeneros(FilmeDoCatalogo dados) {
        return dados.generos().stream()
                .map(referencia -> generos.buscarOuCriar(referencia.idExterno(), referencia.nome()))
                .toList();
    }

    private List<Pessoa> resolverDiretores(CreditosDoCatalogo creditos) {
        return creditos.diretores().stream()
                .map(referencia -> pessoas.buscarOuCriar(referencia.idExterno(), referencia.nome()))
                .toList();
    }

    /** Guarda só os primeiros nomes do crédito: o catálogo não precisa do elenco inteiro. */
    private List<Participacao> resolverElenco(CreditosDoCatalogo creditos) {
        return creditos.elenco().stream()
                .sorted(Comparator.comparing(participacao -> participacao.ordem()))
                .limit(Filme.LIMITE_DE_ELENCO)
                .map(participacao -> new Participacao(
                        pessoas.buscarOuCriar(participacao.idExterno(), participacao.nome()),
                        participacao.personagem(),
                        participacao.ordem()))
                .toList();
    }
}
