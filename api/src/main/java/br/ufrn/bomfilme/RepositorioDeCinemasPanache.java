package br.ufrn.bomfilme;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioDeCinemasPanache implements RepositorioDeCinemas, PanacheRepository<Cinema> {

    @Override
    public List<Cinema> listar(int pagina, int tamanho) {
        return findAll(Sort.by("id"))
                .page(Page.of(pagina, tamanho))
                .list();
    }

    @Override
    public List<Cinema> porRede(long redeId, int pagina, int tamanho) {
        return find("rede.id", Sort.by("id"), redeId)
                .page(Page.of(pagina, tamanho))
                .list();
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
}
