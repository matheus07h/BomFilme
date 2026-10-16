package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.FiltroDeSessoes;
import br.ufrn.bomfilme.dominio.RepositorioDeSessoes;
import br.ufrn.bomfilme.dominio.Sessao;
import br.ufrn.bomfilme.dominio.Pagina;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
class RepositorioDeSessoesPanache implements RepositorioDeSessoes, PanacheRepository<SessaoEntidade> {

    @Override
    public Pagina<Sessao> listar(FiltroDeSessoes filtro, int pagina, int tamanho) {
        List<String> condicoes = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();

        if (filtro.cinemaId() != null) {
            condicoes.add("sala.cinema.id = :cinemaId");
            parametros.put("cinemaId", filtro.cinemaId());
        }
        if (filtro.salaId() != null) {
            condicoes.add("sala.id = :salaId");
            parametros.put("salaId", filtro.salaId());
        }
        if (filtro.filmeId() != null) {
            condicoes.add("filmeId = :filmeId");
            parametros.put("filmeId", filtro.filmeId());
        }

        Sort ordem = Sort.by("inicio").and("id");
        PanacheQuery<SessaoEntidade> consulta = condicoes.isEmpty()
                ? findAll(ordem)
                : find(String.join(" and ", condicoes), ordem, parametros);
        return Paginas.de(consulta, pagina, tamanho, SessaoEntidade::paraDominio);
    }

    @Override
    public Optional<Sessao> porId(long id) {
        return findByIdOptional(id).map(SessaoEntidade::paraDominio);
    }

    @Override
    @Transactional
    public Sessao criar(long salaId, long filmeId, OffsetDateTime inicio, OffsetDateTime fim) {
        SessaoEntidade entidade = new SessaoEntidade(referenciaDaSala(salaId), filmeId, inicio, fim);
        ViolacoesDoBanco.gravando(() -> persistAndFlush(entidade));
        return entidade.paraDominio();
    }

    @Override
    @Transactional
    public Optional<Sessao> atualizar(long id, long salaId, long filmeId,
                                      OffsetDateTime inicio, OffsetDateTime fim) {
        Optional<SessaoEntidade> encontrada = findByIdOptional(id);
        encontrada.ifPresent(entidade -> {
            entidade.sala = referenciaDaSala(salaId);
            entidade.filmeId = filmeId;
            entidade.inicio = inicio;
            entidade.fim = fim;
        });
        ViolacoesDoBanco.gravando(this::flush);
        return encontrada.map(SessaoEntidade::paraDominio);
    }

    @Override
    @Transactional
    public boolean remover(long id) {
        boolean removida = deleteById(id);
        ViolacoesDoBanco.gravando(this::flush);
        return removida;
    }

    @Override
    public boolean existeConflito(long salaId, OffsetDateTime inicio, OffsetDateTime fim, Long ignorarSessaoId) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("salaId", salaId);
        parametros.put("inicio", inicio);
        parametros.put("fim", fim);
        String consulta = "sala.id = :salaId and inicio < :fim and fim > :inicio";
        if (ignorarSessaoId != null) {
            consulta += " and id <> :ignorar";
            parametros.put("ignorar", ignorarSessaoId);
        }
        return count(consulta, parametros) > 0;
    }

    @Override
    public boolean possuiOcupacoes(long sessaoId) {
        return getEntityManager()
                .createQuery("select count(o) from OcupacaoEntidade o where o.sessao.id = :id", Long.class)
                .setParameter("id", sessaoId)
                .getSingleResult() > 0;
    }

    private SalaEntidade referenciaDaSala(long salaId) {
        return getEntityManager().getReference(SalaEntidade.class, salaId);
    }
}
