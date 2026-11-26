package br.ufrn.bomfilme.adaptadores.agendamento;

import br.ufrn.bomfilme.aplicacao.ServicoDeCompras;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Dispara periodicamente a recuperação das compras que ficaram pendentes.
 *
 * <p>Uma compra fica pendente quando o pagamento dá timeout ou o serviço está fora. Depois
 * de {@code bomfilme.compras.prazo-pendente} (10 minutos por padrão), ela é decidida pelo
 * caso de uso: confirmada, recusada ou expirada. A regra mora em {@link ServicoDeCompras};
 * aqui fica só o relógio, que é detalhe do framework.
 */
@ApplicationScoped
class RecuperacaoDeComprasPendentes {

    private static final Logger LOG = Logger.getLogger(RecuperacaoDeComprasPendentes.class);

    @Inject
    ServicoDeCompras compras;

    @ConfigProperty(name = "bomfilme.compras.prazo-pendente", defaultValue = "PT10M")
    Duration prazo;

    @Scheduled(every = "${bomfilme.compras.recuperacao.intervalo:60s}",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void recuperar() {
        int decididas = compras.recuperarPendentesCriadasAntesDe(OffsetDateTime.now().minus(prazo));
        if (decididas > 0) {
            LOG.infof("%d compra(s) pendente(s) decidida(s) pela recuperação", decididas);
        }
    }
}
