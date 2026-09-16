package br.ufrn.bomfilme;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioDeCinemasPanache implements RepositorioDeCinemas, PanacheRepository<Cinema> {

    @Override
    public List<Cinema> listar() {
        return listAll();
    }

    @Override
    public List<Cinema> porRede(long redeId) {
        return list("rede.id", redeId);
    }

    @Override
    public Optional<Cinema> porId(long id) {
        return findByIdOptional(id);
    }

    @Override
    @Transactional
    public Cinema criar(RedeCinema rede, String nome, String cidade, String uf) {
        Cinema cinema = new Cinema(rede, nome, cidade, uf);
        persist(cinema);
        return cinema;
    }
}
