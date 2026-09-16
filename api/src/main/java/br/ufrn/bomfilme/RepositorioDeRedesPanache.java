package br.ufrn.bomfilme;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioDeRedesPanache implements RepositorioDeRedes, PanacheRepository<RedeCinema> {

    @Override
    public List<RedeCinema> listar() {
        return listAll();
    }

    @Override
    public Optional<RedeCinema> porId(long id) {
        return findByIdOptional(id);
    }

    @Override
    @Transactional
    public RedeCinema criar(String nome) {
        RedeCinema rede = new RedeCinema(nome);
        persistAndFlush(rede);
        return rede;
    }
}
