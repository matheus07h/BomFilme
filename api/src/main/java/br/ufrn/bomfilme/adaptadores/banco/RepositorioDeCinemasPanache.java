package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Busca;
import br.ufrn.bomfilme.dominio.Cinema;
import br.ufrn.bomfilme.dominio.FiltroDeCinemas;
import br.ufrn.bomfilme.dominio.RepositorioDeCinemas;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
class RepositorioDeCinemasPanache implements RepositorioDeCinemas, PanacheRepository<CinemaEntidade> {

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

        PanacheQuery<CinemaEntidade> consulta = condicoes.isEmpty()
                ? findAll(Sort.by("id"))
                : find(String.join(" and ", condicoes), Sort.by("id"), parametros);
        return consulta.page(Page.of(pagina, tamanho)).list().stream()
                .map(CinemaEntidade::paraDominio)
                .toList();
    }

    @Override
    public Optional<Cinema> porId(long id) {
        return findByIdOptional(id).map(CinemaEntidade::paraDominio);
    }

    @Override
    @Transactional
    public Cinema criar(long redeId, String nome, String cidade, String uf) {
        CinemaEntidade entidade = new CinemaEntidade(referenciaDaRede(redeId), nome, cidade, uf);
        persistAndFlush(entidade);
        return entidade.paraDominio();
    }

    @Override
    @Transactional
    public Optional<Cinema> atualizar(long id, long redeId, String nome, String cidade, String uf) {
        Optional<CinemaEntidade> encontrado = findByIdOptional(id);
        encontrado.ifPresent(entidade -> {
            entidade.rede = referenciaDaRede(redeId);
            entidade.nome = nome;
            entidade.cidade = cidade;
            entidade.uf = uf;
        });
        flush();
        return encontrado.map(CinemaEntidade::paraDominio);
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
        return contar("select count(s) from SalaEntidade s where s.cinema.id = :id", cinemaId);
    }

    /** Referência sem carregar a rede: o caso de uso já confirmou que ela existe. */
    private RedeCinemaEntidade referenciaDaRede(long redeId) {
        return gerenciador().getReference(RedeCinemaEntidade.class, redeId);
    }

    private boolean contar(String consulta, long id) {
        return gerenciador().createQuery(consulta, Long.class).setParameter("id", id).getSingleResult() > 0;
    }

    private EntityManager gerenciador() {
        return getEntityManager();
    }
}
