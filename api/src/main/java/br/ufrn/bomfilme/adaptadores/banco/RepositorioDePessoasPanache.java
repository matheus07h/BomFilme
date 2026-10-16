package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Pessoa;
import br.ufrn.bomfilme.dominio.RepositorioDePessoas;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
class RepositorioDePessoasPanache implements RepositorioDePessoas, PanacheRepository<PessoaEntidade> {

    @Override
    @Transactional
    public Pessoa buscarOuCriar(long tmdbId, String nome) {
        return entidadePorTmdbId(tmdbId, nome).paraDominio();
    }

    PessoaEntidade entidadePorTmdbId(long tmdbId, String nome) {
        return find("tmdbId", tmdbId).<PessoaEntidade>firstResultOptional()
                .orElseGet(() -> ViolacoesDoBanco.gravando(() -> {
                    PessoaEntidade entidade = new PessoaEntidade(tmdbId, nome);
                    persist(entidade);
                    return entidade;
                }));
    }
}
