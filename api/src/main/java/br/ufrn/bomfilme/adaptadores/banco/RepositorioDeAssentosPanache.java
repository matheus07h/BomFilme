package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Assento;
import br.ufrn.bomfilme.dominio.Busca;
import br.ufrn.bomfilme.dominio.RepositorioDeAssentos;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
class RepositorioDeAssentosPanache implements RepositorioDeAssentos, PanacheRepository<AssentoEntidade> {

    @Override
    public List<Assento> porSala(long salaId) {
        return find("sala.id", Sort.by("fileira").and("numero"), salaId).list().stream()
                .map(AssentoEntidade::paraDominio)
                .toList();
    }

    @Override
    public List<Assento> listarDaSala(long salaId, String fileira, int pagina, int tamanho) {
        Sort ordem = Sort.by("fileira").and("numero").and("id");
        PanacheQuery<AssentoEntidade> consulta = Busca.vazia(fileira)
                ? find("sala.id = ?1", ordem, salaId)
                : find("sala.id = ?1 and fileira = ?2", ordem, salaId, fileira);
        return consulta.page(Page.of(pagina, tamanho)).list().stream()
                .map(AssentoEntidade::paraDominio)
                .toList();
    }

    @Override
    public Optional<Assento> porId(long id) {
        return findByIdOptional(id).map(AssentoEntidade::paraDominio);
    }

    @Override
    @Transactional
    public Assento criar(long salaId, String fileira, int numero) {
        AssentoEntidade entidade = new AssentoEntidade(
                getEntityManager().getReference(SalaEntidade.class, salaId), fileira, numero);
        persistAndFlush(entidade);
        return entidade.paraDominio();
    }

    @Override
    @Transactional
    public Optional<Assento> atualizar(long id, String fileira, int numero) {
        Optional<AssentoEntidade> encontrado = findByIdOptional(id);
        encontrado.ifPresent(entidade -> {
            entidade.fileira = fileira;
            entidade.numero = numero;
        });
        flush();
        return encontrado.map(AssentoEntidade::paraDominio);
    }

    @Override
    @Transactional
    public boolean remover(long id) {
        boolean removido = deleteById(id);
        flush();
        return removido;
    }

    @Override
    public boolean possuiOcupacoes(long assentoId) {
        return getEntityManager()
                .createQuery("select count(o) from OcupacaoEntidade o where o.assento.id = :id", Long.class)
                .setParameter("id", assentoId)
                .getSingleResult() > 0;
    }

    @Override
    public Set<Long> ocupadosNaSessao(long sessaoId) {
        return getEntityManager()
                .createQuery("select o.assento.id from OcupacaoEntidade o where o.sessao.id = :id", Long.class)
                .setParameter("id", sessaoId)
                .getResultStream()
                .collect(Collectors.toSet());
    }

    /**
     * A unicidade de (sessão, assento) fica no banco, o que recusa a segunda de duas
     * ocupações simultâneas do mesmo assento. O flush faz a violação aparecer aqui.
     */
    @Override
    @Transactional
    public void ocupar(long sessaoId, long assentoId) {
        getEntityManager().persist(new OcupacaoEntidade(
                getEntityManager().getReference(SessaoEntidade.class, sessaoId),
                getEntityManager().getReference(AssentoEntidade.class, assentoId)));
        getEntityManager().flush();
    }
}
