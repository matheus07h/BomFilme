package br.ufrn.bomfilme;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class RepositorioDePessoasPanache implements RepositorioDePessoas, PanacheRepository<Pessoa> {

    @Override
    public Optional<Pessoa> buscarPorTmdbId(Long tmdbId) {
        return find("tmdbId", tmdbId).firstResultOptional();
    }
    @Transactional
    @Override
    public void persistir(Pessoa pessoa) {
        persist(pessoa);
    }
}
