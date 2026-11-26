package br.ufrn.bomfilme.compras;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufrn.bomfilme.Fixtures;
import br.ufrn.bomfilme.aplicacao.ServicoDeCompras;
import br.ufrn.bomfilme.dominio.Assento;
import br.ufrn.bomfilme.dominio.Compra;
import br.ufrn.bomfilme.dominio.RepositorioDeAssentos;
import br.ufrn.bomfilme.dominio.RepositorioDeCompras;
import br.ufrn.bomfilme.dominio.Sala;
import br.ufrn.bomfilme.dominio.Sessao;
import br.ufrn.bomfilme.dominio.StatusDaCompra;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Base dos testes do fluxo de compra contra o serviço Go de verdade (issue #8).
 *
 * <p>Cada subclasse sobe o contêiner do serviço Go num cenário (aprovação, recusa, demora
 * ou falha) e compra um assento pelo caso de uso ou pela rota HTTP. As verificações releem
 * a compra, o ingresso e a reserva do banco, em vez de confiar no objeto devolvido.
 *
 * <p>O estado da reserva é lido com SQL direto porque nenhuma porta o expõe, e as
 * entidades JPA não são visíveis daqui.
 */
abstract class CenarioDeCompra {

    @Inject
    Fixtures dados;

    @Inject
    ServicoDeCompras compras;

    @Inject
    RepositorioDeCompras repositorio;

    @Inject
    RepositorioDeAssentos assentos;

    @Inject
    EntityManager banco;

    Sessao sessao;
    Assento assento;

    void prepararAssentoLivre() {
        Sala sala = dados.sala();
        assento = dados.assento(sala, "C", 7);
        sessao = dados.sessao(sala);
    }

    Compra comprarAssentoLivre() {
        prepararAssentoLivre();
        return compras.comprar(sessao.id(), assento.id());
    }

    ValidatableResponse comprarPelaRota() {
        prepararAssentoLivre();
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("assentoId", assento.id()))
                .when().post("/sessoes/{id}/compras", sessao.id())
                .then();
    }

    /**
     * Uma compra pendente criada direto no repositório, sem passar pelo pagamento: é o
     * estado que sobra depois de um timeout, e é o que a recuperação decide.
     */
    Compra compraPendente() {
        prepararAssentoLivre();
        return repositorio.reservar(sessao.id(), assento.id(), UUID.randomUUID(), 2_500);
    }

    /** Roda a recuperação como se o prazo de todas as compras já tivesse passado. */
    int recuperarTodasAsPendentes() {
        return compras.recuperarPendentesCriadasAntesDe(OffsetDateTime.now().plusMinutes(1));
    }

    void assertStatusGravado(Compra compra, StatusDaCompra esperado) {
        assertEquals(esperado, compras.porId(compra.id()).status());
    }

    void assertSemIngresso(Compra compra) {
        assertTrue(compras.ingressoDaCompra(compra.id()).isEmpty(),
                "não deveria haver ingresso para a compra " + compra.id());
    }

    boolean assentoOcupado() {
        return assentos.ocupadosNaSessao(sessao.id()).contains(assento.id());
    }

    /** Estado da linha de assento_ocupado da reserva da compra; vazio se ela foi liberada. */
    Optional<String> estadoDaReserva(Compra compra) {
        return banco.createNativeQuery(
                        "select estado from assento_ocupado where reserva_id = :id", String.class)
                .setParameter("id", compra.reservaId())
                .getResultStream()
                .findFirst()
                .map(String.class::cast);
    }

    void assertAssentoVendido(Compra compra) {
        assertTrue(assentoOcupado(), "o assento deveria estar ocupado");
        assertEquals(Optional.of("CONFIRMADO"), estadoDaReserva(compra));
    }

    void assertAssentoReservado(Compra compra) {
        assertTrue(assentoOcupado(), "o assento deveria continuar reservado");
        assertEquals(Optional.of("RESERVADO"), estadoDaReserva(compra));
    }

    void assertAssentoLivre(Compra compra) {
        assertFalse(assentoOcupado(), "o assento deveria ter sido liberado");
        assertEquals(Optional.empty(), estadoDaReserva(compra));
    }
}
