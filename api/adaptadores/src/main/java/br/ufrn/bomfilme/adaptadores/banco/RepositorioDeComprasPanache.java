package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Compra;
import br.ufrn.bomfilme.dominio.ErroDeDominio;
import br.ufrn.bomfilme.dominio.Ingresso;
import br.ufrn.bomfilme.dominio.RepositorioDeAssentos;
import br.ufrn.bomfilme.dominio.RepositorioDeCompras;
import br.ufrn.bomfilme.dominio.StatusDaCompra;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

/**
 * A reserva do assento é a da issue #7: este repositório usa {@link RepositorioDeAssentos}
 * para reservar, confirmar e liberar, em vez de repetir as consultas dele. Os métodos de
 * lá entram na transação daqui, então a compra e a reserva mudam sempre juntas.
 */
@ApplicationScoped
class RepositorioDeComprasPanache implements RepositorioDeCompras, PanacheRepository<CompraEntidade> {

    @Inject
    RepositorioDeAssentos assentos;

    /**
     * Se a reserva for recusada pela unicidade de (sessão, assento), a transação inteira
     * volta e não sobra compra órfã.
     */
    @Override
    @Transactional
    public Compra reservar(long sessaoId, long assentoId, UUID reservaId, long valorCentavos) {
        assentos.reservar(sessaoId, assentoId, reservaId);
        var em = getEntityManager();
        CompraEntidade compra = new CompraEntidade(
                em.getReference(SessaoEntidade.class, sessaoId),
                em.getReference(AssentoEntidade.class, assentoId),
                reservaId, valorCentavos);
        ViolacoesDoBanco.gravando(() -> {
            em.persist(compra);
            em.flush();
        });
        return compra.paraDominio();
    }

    @Override
    public Optional<Compra> porId(long id) {
        return findByIdOptional(id).map(CompraEntidade::paraDominio);
    }

    /**
     * A troca de status é condicional ({@code where status = PENDENTE}), então de duas
     * confirmações da mesma compra só uma altera a linha e só ela emite ingresso.
     */
    @Override
    @Transactional
    public Optional<Ingresso> confirmar(long compraId) {
        Optional<UUID> reservaId = reservaDaCompraPendente(compraId, StatusDaCompra.CONFIRMADA);
        if (reservaId.isEmpty()) {
            return Optional.empty();
        }
        if (!assentos.confirmarReserva(reservaId.get())) {
            throw reservaForaDoEstado(compraId);
        }
        IngressoEntidade ingresso = new IngressoEntidade(
                getEntityManager().getReference(CompraEntidade.class, compraId));
        ViolacoesDoBanco.gravando(() -> {
            getEntityManager().persist(ingresso);
            getEntityManager().flush();
        });
        return Optional.of(ingresso.paraDominio());
    }

    @Override
    @Transactional
    public boolean recusar(long compraId) {
        Optional<UUID> reservaId = reservaDaCompraPendente(compraId, StatusDaCompra.RECUSADA);
        if (reservaId.isEmpty()) {
            return false;
        }
        if (!assentos.liberarReserva(reservaId.get())) {
            throw reservaForaDoEstado(compraId);
        }
        return true;
    }

    @Override
    public Optional<Ingresso> ingressoDaCompra(long compraId) {
        return getEntityManager()
                .createQuery("from IngressoEntidade i where i.compra.id = :id", IngressoEntidade.class)
                .setParameter("id", compraId)
                .getResultStream()
                .findFirst()
                .map(IngressoEntidade::paraDominio);
    }

    /** Tira a compra de PENDENTE e devolve a reserva dela; vazio se a compra não estava pendente. */
    private Optional<UUID> reservaDaCompraPendente(long compraId, StatusDaCompra novo) {
        Optional<UUID> reservaId = findByIdOptional(compraId).map(compra -> compra.reservaId);
        boolean saiu = update("status = ?1 where id = ?2 and status = ?3",
                novo, compraId, StatusDaCompra.PENDENTE) == 1;
        return saiu ? reservaId : Optional.empty();
    }

    /**
     * Uma compra pendente com a reserva já confirmada ou liberada é estado que o fluxo não
     * produz. A exceção desfaz a troca de status, para a compra não mudar sozinha.
     */
    private static ErroDeDominio.Conflito reservaForaDoEstado(long compraId) {
        return new ErroDeDominio.Conflito("a reserva da compra " + compraId + " não está pendente");
    }
}
