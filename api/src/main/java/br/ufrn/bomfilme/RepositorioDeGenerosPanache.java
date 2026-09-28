package br.ufrn.bomfilme;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class RepositorioDeGenerosPanache implements RepositorioDeGeneros, PanacheRepository<Genero> {

    @Override
    public Optional<Genero> buscarPorTmdbId(long tmdbId) {
        return find("tmdbId", tmdbId).firstResultOptional();
    }

    @Override
    @Transactional
    public void persistir(Genero genero) {
        persist(genero);
    }
}