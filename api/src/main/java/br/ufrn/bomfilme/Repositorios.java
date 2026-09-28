package br.ufrn.bomfilme;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

interface RepositorioDeSessoes {

    Optional<Sessao> porId(long id);
}

@ApplicationScoped
class RepositorioDeSessoesPanache implements RepositorioDeSessoes, PanacheRepository<Sessao> {

    @Override
    public Optional<Sessao> porId(long id) {
        return findByIdOptional(id);
    }
}

interface RepositorioDeAssentos {

    List<Assento> porSala(long salaId);

    Optional<Assento> porId(long id);

    Set<Long> ocupadosNaSessao(long sessaoId);

    void ocupar(Sessao sessao, Assento assento);
}

@ApplicationScoped
class RepositorioDeAssentosPanache implements RepositorioDeAssentos, PanacheRepository<Assento> {

    @Override
    public List<Assento> porSala(long salaId) {
        return find("sala.id", Sort.by("fileira").and("numero"), salaId).list();
    }

    @Override
    public Optional<Assento> porId(long id) {
        return findByIdOptional(id);
    }

    @Override
    public Set<Long> ocupadosNaSessao(long sessaoId) {
        return getEntityManager()
                .createQuery("select o.assento.id from Ocupacao o where o.sessao.id = :sessaoId", Long.class)
                .setParameter("sessaoId", sessaoId)
                .getResultStream()
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void ocupar(Sessao sessao, Assento assento) {
        getEntityManager().persist(new Ocupacao(sessao, assento));
        getEntityManager().flush();
    }
}
