package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Busca;
import br.ufrn.bomfilme.dominio.FiltroDeSalas;
import br.ufrn.bomfilme.dominio.RepositorioDeSalas;
import br.ufrn.bomfilme.dominio.Sala;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
class RepositorioDeSalasPanache implements RepositorioDeSalas, PanacheRepository<SalaEntidade> {

    @Override
    public List<Sala> listar(FiltroDeSalas filtro, int pagina, int tamanho) {
        List<String> condicoes = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();

        if (filtro.cinemaId() != null) {
            condicoes.add("cinema.id = :cinemaId");
            parametros.put("cinemaId", filtro.cinemaId());
        }
        if (!Busca.vazia(filtro.nome())) {
            condicoes.add("lower(nome) like :nome escape '\\'");
            parametros.put("nome", Busca.contendo(filtro.nome()));
        }

        Sort ordem = Sort.by("id");
        PanacheQuery<SalaEntidade> consulta = condicoes.isEmpty()
                ? findAll(ordem)
                : find(String.join(" and ", condicoes), ordem, parametros);
        return consulta.page(Page.of(pagina, tamanho)).list().stream()
                .map(SalaEntidade::paraDominio)
                .toList();
    }

    @Override
    public Optional<Sala> porId(long id) {
        return findByIdOptional(id).map(SalaEntidade::paraDominio);
    }

    @Override
    @Transactional
    public Sala criar(long cinemaId, String nome) {
        SalaEntidade entidade = new SalaEntidade(referenciaDoCinema(cinemaId), nome);
        persistAndFlush(entidade);
        return entidade.paraDominio();
    }

    @Override
    @Transactional
    public Optional<Sala> atualizar(long id, long cinemaId, String nome) {
        Optional<SalaEntidade> encontrada = findByIdOptional(id);
        encontrada.ifPresent(entidade -> {
            entidade.cinema = referenciaDoCinema(cinemaId);
            entidade.nome = nome;
        });
        flush();
        return encontrada.map(SalaEntidade::paraDominio);
    }

    @Override
    @Transactional
    public boolean remover(long id) {
        boolean removida = deleteById(id);
        flush();
        return removida;
    }

    @Override
    public boolean possuiAssentos(long salaId) {
        return contar("select count(a) from AssentoEntidade a where a.sala.id = :id", salaId);
    }

    @Override
    public boolean possuiSessoes(long salaId) {
        return contar("select count(s) from SessaoEntidade s where s.sala.id = :id", salaId);
    }

    private CinemaEntidade referenciaDoCinema(long cinemaId) {
        return getEntityManager().getReference(CinemaEntidade.class, cinemaId);
    }

    private boolean contar(String consulta, long id) {
        return getEntityManager().createQuery(consulta, Long.class)
                .setParameter("id", id)
                .getSingleResult() > 0;
    }
}
