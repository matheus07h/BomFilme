package br.ufrn.bomfilme;

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
public class RepositorioDeSalasPanache implements RepositorioDeSalas, PanacheRepository<Sala> {

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

        PanacheQuery<Sala> consulta = condicoes.isEmpty()
                ? findAll(Sort.by("id"))
                : find(String.join(" and ", condicoes), Sort.by("id"), parametros);
        return consulta.page(Page.of(pagina, tamanho)).list();
    }

    @Override
    public Optional<Sala> porId(long id) {
        return findByIdOptional(id);
    }

    @Override
    @Transactional
    public Sala criar(Cinema cinema, String nome) {
        Sala sala = new Sala(cinema, nome);
        persistAndFlush(sala);
        return sala;
    }

    @Override
    @Transactional
    public Optional<Sala> atualizar(long id, Cinema cinema, String nome) {
        Optional<Sala> encontrada = findByIdOptional(id);
        encontrada.ifPresent(sala -> {
            sala.cinema = cinema;
            sala.nome = nome;
        });
        flush();
        return encontrada;
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
        return contar("select count(a) from Assento a where a.sala.id = :salaId", salaId) > 0;
    }

    @Override
    public boolean possuiSessoes(long salaId) {
        return contar("select count(s) from Sessao s where s.sala.id = :salaId", salaId) > 0;
    }

    private long contar(String jpql, long salaId) {
        return getEntityManager()
                .createQuery(jpql, Long.class)
                .setParameter("salaId", salaId)
                .getSingleResult();
    }
}
