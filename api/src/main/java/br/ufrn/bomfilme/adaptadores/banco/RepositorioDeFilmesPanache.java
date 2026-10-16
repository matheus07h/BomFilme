package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Busca;
import br.ufrn.bomfilme.dominio.Filme;
import br.ufrn.bomfilme.dominio.FiltroDeFilmes;
import br.ufrn.bomfilme.dominio.Pagina;
import br.ufrn.bomfilme.dominio.Participacao;
import br.ufrn.bomfilme.dominio.RepositorioDeFilmes;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
class RepositorioDeFilmesPanache implements RepositorioDeFilmes, PanacheRepository<FilmeEntidade> {

    /**
     * Campos pelos quais a listagem aceita ordenar. A consulta usa o valor desta tabela,
     * nunca o texto que veio na requisição, o que impede injeção pelo parâmetro de ordenação.
     */
    private static final Map<String, String> CAMPOS_ORDENAVEIS = Map.of(
            "titulo", "titulo",
            "lancamento", "dataLancamento",
            "nota", "notaMediaTmdb",
            "popularidade", "popularidade");

    @Inject
    RepositorioDeGenerosPanache generos;

    @Inject
    RepositorioDePessoasPanache pessoas;

    @Override
    public Optional<Filme> porId(long id) {
        return findByIdOptional(id).map(FilmeEntidade::paraDominio);
    }

    @Override
    public Optional<Filme> porTmdbId(long tmdbId) {
        return find("tmdbId", tmdbId).<FilmeEntidade>firstResultOptional().map(FilmeEntidade::paraDominio);
    }

    @Override
    public Pagina<Filme> listar(FiltroDeFilmes filtro, int pagina, int tamanho) {
        StringBuilder hql = new StringBuilder("from FilmeEntidade f where 1 = 1");
        Map<String, Object> parametros = new HashMap<>();

        if (!Busca.vazia(filtro.titulo())) {
            hql.append(" and (lower(f.titulo) like :titulo escape '\\'")
                    .append(" or lower(f.tituloOriginal) like :titulo escape '\\')");
            parametros.put("titulo", Busca.contendo(filtro.titulo()));
        }
        if (filtro.generoId() != null) {
            hql.append(" and exists (select 1 from f.generos g where g.id = :generoId)");
            parametros.put("generoId", filtro.generoId());
        }
        if (filtro.ano() != null) {
            hql.append(" and year(f.dataLancamento) = :ano");
            parametros.put("ano", filtro.ano());
        }

        String campo = CAMPOS_ORDENAVEIS.getOrDefault(filtro.ordenarPor(), "titulo");
        String direcao = "desc".equalsIgnoreCase(filtro.direcao()) ? "desc" : "asc";
        hql.append(" order by f.").append(campo).append(' ').append(direcao).append(" nulls last");

        PanacheQuery<FilmeEntidade> consulta = find(hql.toString(), parametros).page(Page.of(pagina, tamanho));
        return Pagina.de(
                consulta.list().stream().map(FilmeEntidade::paraDominio).toList(),
                pagina, tamanho, consulta.count());
    }

    @Override
    @Transactional
    public Filme salvar(Filme filme) {
        return ViolacoesDoBanco.gravando(() -> gravar(filme));
    }

    private Filme gravar(Filme filme) {
        FilmeEntidade entidade = find("tmdbId", filme.tmdbId()).<FilmeEntidade>firstResultOptional()
                .orElseGet(FilmeEntidade::new);

        entidade.tmdbId = filme.tmdbId();
        entidade.titulo = filme.titulo();
        entidade.tituloOriginal = filme.tituloOriginal();
        entidade.sinopse = filme.sinopse();
        entidade.posterPath = filme.posterPath();
        entidade.duracaoMinutos = filme.duracaoMinutos();
        entidade.dataLancamento = filme.dataLancamento();
        entidade.notaMediaTmdb = filme.notaMediaTmdb();
        entidade.popularidade = filme.popularidade();

        if (entidade.id == null) {
            persist(entidade);
        }

        substituirGeneros(entidade, filme);
        substituirDiretores(entidade, filme);
        substituirElenco(entidade, filme);

        flush();
        return entidade.paraDominio();
    }

    @Override
    @Transactional
    public boolean remover(long id) {
        boolean removido = deleteById(id);
        ViolacoesDoBanco.gravando(this::flush);
        return removido;
    }

    @Override
    @Transactional
    public boolean atualizarPopularidade(long tmdbId, Double popularidade) {
        Optional<FilmeEntidade> encontrado = find("tmdbId", tmdbId).firstResultOptional();
        encontrado.ifPresent(entidade -> entidade.popularidade = popularidade);
        ViolacoesDoBanco.gravando(this::flush);
        return encontrado.isPresent();
    }

    private void substituirGeneros(FilmeEntidade entidade, Filme filme) {
        Set<GeneroEntidade> novos = filme.generos().stream()
                .map(genero -> generos.entidadePorTmdbId(genero.tmdbId(), genero.nome()))
                .collect(Collectors.toSet());
        entidade.generos.clear();
        entidade.generos.addAll(novos);
    }

    private void substituirDiretores(FilmeEntidade entidade, Filme filme) {
        Set<PessoaEntidade> novos = filme.diretores().stream()
                .map(pessoa -> pessoas.entidadePorTmdbId(pessoa.tmdbId(), pessoa.nome()))
                .collect(Collectors.toSet());
        entidade.diretores.clear();
        entidade.diretores.addAll(novos);
    }

    private void substituirElenco(FilmeEntidade entidade, Filme filme) {
        entidade.elenco.clear();
        for (Participacao participacao : filme.elencoPorOrdem()) {
            PessoaEntidade pessoa = pessoas.entidadePorTmdbId(
                    participacao.pessoa().tmdbId(), participacao.pessoa().nome());
            entidade.elenco.add(new ElencoFilmeEntidade(
                    entidade, pessoa, participacao.personagem(), participacao.ordem()));
        }
    }
}
