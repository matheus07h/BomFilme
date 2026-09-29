package br.ufrn.bomfilme;

import br.ufrn.bomfilme.utils.FiltroDeFilmes;
import br.ufrn.bomfilme.utils.Pagina;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class RepositorioDeFilmesPanache implements  RepositorioDeFilmes, PanacheRepository<Filme> {
    private static final Map<String, String> CAMPOS_ORDENAVEIS = Map.of(
            "titulo", "titulo",
            "lancamento", "dataLancamento",
            "nota", "notaMediaTmdb",
            "popularidade", "popularidade");


    @Override
    public Optional<Filme> buscarPorId(long id) {
        return findByIdOptional(id);
    }

    @Override
    public Pagina<Filme> buscarPorFiltro(FiltroDeFilmes filtro, int pagina, int tamanho){
        StringBuilder hql = new StringBuilder("from Filme f where 1 = 1");
        Map<String, Object> params = new HashMap<>();

        if (filtro.titulo() != null && !filtro.titulo().isBlank()){
            hql.append(" and (lower(f.titulo) like :titulo or lower(f.tituloOriginal) like :titulo)");
            params.put("titulo", "%" + filtro.titulo().trim().toLowerCase() + "%");
        }
        if (filtro.generoId() != null) {
            hql.append(" and exists (select 1 from f.generos g where g.id = :generoId)");
            params.put("generoId", filtro.generoId());
        }
        if (filtro.ano() != null) {
            hql.append(" and year(f.dataLancamento) = :ano");
            params.put("ano", filtro.ano());
        }

        String campo = CAMPOS_ORDENAVEIS.getOrDefault(filtro.ordenarPor(), "titulo");
        String direcaoSql = "desc".equalsIgnoreCase(filtro.direcao()) ? "desc" : "asc";
        hql.append(" order by f.").append(campo).append(' ').append(direcaoSql).append(" nulls last");
        PanacheQuery<Filme> consulta = find(hql.toString(), params).page(Page.of(pagina, tamanho));
        return Pagina.de(consulta.list(), pagina, tamanho, consulta.count());
    }

    @Override
    public Optional<Filme> buscarPorTmdbId(long tmdbId) {
        return find("tmdbId", tmdbId).firstResultOptional();
    }

    @Override
    @Transactional
    public void persistir(Filme filme) {
        persist(filme);
    }

    @Override
    @Transactional
    public boolean deletar(long id){
        return deleteById(id);
    }
}
