package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Genero;
import br.ufrn.bomfilme.dominio.RepositorioDeGeneros;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

@ApplicationScoped
class RepositorioDeGenerosPanache implements RepositorioDeGeneros, PanacheRepository<GeneroEntidade> {

    @Override
    public List<Genero> listarTodos() {
        return listAll(Sort.by("nome")).stream().map(GeneroEntidade::paraDominio).toList();
    }

    @Override
    @Transactional
    public Genero buscarOuCriar(long tmdbId, String nome) {
        return entidadePorTmdbId(tmdbId, nome).paraDominio();
    }

    GeneroEntidade entidadePorTmdbId(long tmdbId, String nome) {
        return find("tmdbId", tmdbId).<GeneroEntidade>firstResultOptional()
                .orElseGet(() -> ViolacoesDoBanco.gravando(() -> {
                    GeneroEntidade entidade = new GeneroEntidade(tmdbId, nome);
                    persist(entidade);
                    eventosDeGenero.fire(new GeneroCriado());
                    return entidade;
                }));
    }

    @Inject
    Event<GeneroCriado> eventosDeGenero;

    @Inject
    @CacheName("generos")
    Cache cacheDeGeneros;

    record GeneroCriado() {}

    void invalidarCacheDeGeneros(
            @Observes(during = TransactionPhase.AFTER_SUCCESS) GeneroCriado evento) {
        cacheDeGeneros.invalidateAll().await().indefinitely();
    }
}
