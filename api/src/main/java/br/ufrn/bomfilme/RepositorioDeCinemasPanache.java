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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class RepositorioDeCinemasPanache implements RepositorioDeCinemas, PanacheRepository<Cinema> {

    @Override
    public List<Cinema> listar(FiltroDeCinemas filtro, int pagina, int tamanho) {
        List<String> condicoes = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();

        if (filtro.redeId() != null) {
            condicoes.add("rede.id = :redeId");
            parametros.put("redeId", filtro.redeId());
        }
        if (!Busca.vazia(filtro.nome())) {
            condicoes.add("lower(nome) like :nome escape '\\'");
            parametros.put("nome", Busca.contendo(filtro.nome()));
        }
        if (!Busca.vazia(filtro.cidade())) {
            condicoes.add("lower(cidade) like :cidade escape '\\'");
            parametros.put("cidade", Busca.contendo(filtro.cidade()));
        }
        if (!Busca.vazia(filtro.uf())) {
            condicoes.add("uf = :uf");
            parametros.put("uf", filtro.uf().strip().toUpperCase(Locale.ROOT));
        }

        PanacheQuery<Cinema> consulta = condicoes.isEmpty()
                ? findAll(Sort.by("id"))
                : find(String.join(" and ", condicoes), Sort.by("id"), parametros);
        return consulta.page(Page.of(pagina, tamanho)).list();
    }

    @Override
    public Optional<Cinema> porId(long id) {
        return findByIdOptional(id);
    }

    @Override
    @Transactional
    public Cinema criar(RedeCinema rede, String nome, String cidade, String uf) {
        Cinema cinema = new Cinema(rede, nome, cidade, uf);
        persistAndFlush(cinema);
        return cinema;
    }

    @Override
    @Transactional
    public Optional<Cinema> atualizar(long id, RedeCinema rede, String nome, String cidade, String uf) {
        Optional<Cinema> encontrado = findByIdOptional(id);
        encontrado.ifPresent(cinema -> {
            cinema.rede = rede;
            cinema.nome = nome;
            cinema.cidade = cidade;
            cinema.uf = uf;
        });
        flush();
        return encontrado;
    }

    @Override
    @Transactional
    public boolean remover(long id) {
        boolean removido = deleteById(id);
        flush();
        return removido;
    }

    @Override
    public boolean existePorRede(long redeId) {
        return count("rede.id", redeId) > 0;
    }

    @Override
    public boolean possuiSalas(long cinemaId) {
        Long salas = getEntityManager()
                .createQuery("select count(s) from Sala s where s.cinema.id = :cinemaId", Long.class)
                .setParameter("cinemaId", cinemaId)
                .getSingleResult();
        return salas > 0;
    }
}
