package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Genero;
import br.ufrn.bomfilme.dominio.RepositorioDeGeneros;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

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
                    return entidade;
                }));
    }
}
