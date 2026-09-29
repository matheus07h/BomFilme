package br.ufrn.bomfilme;

import br.ufrn.bomfilme.dtos.response.GeneroResponse;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
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

    @Override
    public List<Genero> listarTodos(){
        return listAll(Sort.by("nome"));
    }
}