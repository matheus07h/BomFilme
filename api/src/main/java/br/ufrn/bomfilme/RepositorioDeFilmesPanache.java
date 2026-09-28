package br.ufrn.bomfilme;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioDeFilmesPanache implements  RepositorioDeFilmes, PanacheRepository<Filme> {
    @Override
    public List<Filme> listar(int pagina, int tamanho) {
        return findAll(Sort.by("id")).page(Page.of(pagina,tamanho)).list();
    }

    @Override
    public Optional<Filme> buscarPorId(long id) {
        return findByIdOptional(id);
    }

    @Override
    public List<Filme> buscarPorTitulo(String titulo, int pagina, int tamanho) {
        return find("lower(titulo) like lower(?1)", Sort.by("titulo"), "%" + titulo + "%")
                .page(Page.of(pagina, tamanho))
                .list();
    }

    @Override
    public Optional<Filme> buscarPorTmdbId(long tmdbId) {
        return find("tmdbId", tmdbId).firstResultOptional();
    }

    @Override
    @Transactional
    public void persistir(Filme filme) {
        persist(filme);
    }
}
