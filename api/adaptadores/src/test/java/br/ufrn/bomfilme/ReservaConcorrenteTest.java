package br.ufrn.bomfilme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufrn.bomfilme.aplicacao.ServicoDeSessoes;
import br.ufrn.bomfilme.dominio.Assento;
import br.ufrn.bomfilme.dominio.ErroDeDominio;
import br.ufrn.bomfilme.dominio.ReservaDeAssento;
import br.ufrn.bomfilme.dominio.Sala;
import br.ufrn.bomfilme.dominio.Sessao;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ReservaConcorrenteTest {

    @Inject
    Fixtures dados;

    @Inject
    ServicoDeSessoes servico;

    @Test
    void somenteUmaDeDuasSolicitacoesConfirmaOMesmoAssento() throws Exception {
        Sala sala = dados.sala();
        Assento assento = dados.assento(sala, "A", 1);
        Sessao sessao = dados.sessao(sala);
        CountDownLatch inicio = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<ReservaDeAssento>> disputas = List.of(
                    executor.submit(() -> disputar(inicio, sessao.id(), assento.id())),
                    executor.submit(() -> disputar(inicio, sessao.id(), assento.id())));
            inicio.countDown();

            ReservaDeAssento primeira = disputas.get(0).get(15, TimeUnit.SECONDS);
            ReservaDeAssento segunda = disputas.get(1).get(15, TimeUnit.SECONDS);
            assertEquals(1, (primeira == null ? 0 : 1) + (segunda == null ? 0 : 1));

            ReservaDeAssento vencedora = primeira == null ? segunda : primeira;
            assertFalse(servico.assentosDaSessao(sessao.id()).getFirst().disponivel());
            servico.confirmarReserva(vencedora.id());
            assertThrows(ErroDeDominio.Conflito.class,
                    () -> servico.liberarReserva(vencedora.id()));
            assertThrows(ErroDeDominio.Conflito.class,
                    () -> servico.reservarAssento(sessao.id(), assento.id()));
            assertFalse(servico.assentosDaSessao(sessao.id()).getFirst().disponivel());
        }
    }

    @Test
    void reservaBloqueiaConcorrenteERecusaDePagamentoLiberaAssento() {
        Sala sala = dados.sala();
        Assento assento = dados.assento(sala, "B", 1);
        Sessao sessao = dados.sessao(sala);

        ReservaDeAssento reserva = servico.reservarAssento(sessao.id(), assento.id());
        assertFalse(servico.assentosDaSessao(sessao.id()).getFirst().disponivel());
        assertThrows(ErroDeDominio.Conflito.class,
                () -> servico.reservarAssento(sessao.id(), assento.id()));
        servico.liberarReserva(reserva.id());
        assertTrue(servico.assentosDaSessao(sessao.id()).getFirst().disponivel());
        assertThrows(ErroDeDominio.Conflito.class,
                () -> servico.confirmarReserva(reserva.id()));

        ReservaDeAssento outra = servico.reservarAssento(sessao.id(), assento.id());
        servico.confirmarReserva(outra.id());
        assertFalse(servico.assentosDaSessao(sessao.id()).getFirst().disponivel());

        Sessao outraSessao = dados.sessao(sala, dados.filme(), sessao.fim().plusHours(1));
        ReservaDeAssento emOutraSessao = servico.reservarAssento(outraSessao.id(), assento.id());
        assertNotNull(emOutraSessao.id());
        Assento deOutraSala = dados.assento(dados.sala(), "C", 1);
        assertThrows(ErroDeDominio.NaoEncontrado.class,
                () -> servico.reservarAssento(sessao.id(), deOutraSala.id()));
    }

    private ReservaDeAssento disputar(CountDownLatch inicio, long sessaoId, long assentoId)
            throws InterruptedException {
        inicio.await();
        try {
            return servico.reservarAssento(sessaoId, assentoId);
        } catch (ErroDeDominio.Conflito esperado) {
            return null;
        }
    }
}
