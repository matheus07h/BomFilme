package br.ufrn.bomfilme.compras;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufrn.bomfilme.adaptadores.pagamentos.ServidorGoDeTeste;
import br.ufrn.bomfilme.dominio.Compra;
import br.ufrn.bomfilme.dominio.ErroDeDominio;
import br.ufrn.bomfilme.dominio.Ingresso;
import br.ufrn.bomfilme.dominio.Sala;
import br.ufrn.bomfilme.dominio.StatusDaCompra;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(
        value = ServidorGoDeTeste.class,
        initArgs = @ResourceArg(name = "cenario", value = "aprovacao"),
        restrictToAnnotatedClass = true)
class CompraAprovadaTest extends CenarioDeCompra {

    @Test
    void aprovacaoConfirmaACompraEEmiteOIngresso() {
        Compra compra = comprarAssentoLivre();

        assertEquals(StatusDaCompra.CONFIRMADA, compra.status());
        assertStatusGravado(compra, StatusDaCompra.CONFIRMADA);
        Ingresso ingresso = compras.ingressoDaCompra(compra.id()).orElseThrow();
        assertEquals(compra.id(), ingresso.compraId());
        assertNotNull(ingresso.codigo());
        assertAssentoVendido(compra);
    }

    @Test
    void compraConfirmadaNaoGanhaSegundoIngresso() {
        Compra compra = comprarAssentoLivre();
        Ingresso primeiro = compras.ingressoDaCompra(compra.id()).orElseThrow();

        assertTrue(repositorio.confirmar(compra.id()).isEmpty());
        assertEquals(primeiro, compras.ingressoDaCompra(compra.id()).orElseThrow());
    }

    @Test
    void assentoJaVendidoNaoPodeSerCompradoDeNovo() {
        comprarAssentoLivre();

        var erro = assertThrows(ErroDeDominio.Conflito.class,
                () -> compras.comprar(sessao.id(), assento.id()));
        assertEquals("assento " + assento.id() + " já está ocupado nesta sessão", erro.getMessage());
    }

    @Test
    void rotaDevolveACompraConfirmadaComOIngresso() {
        int compraId = comprarPelaRota()
                .statusCode(201)
                .header("Location", containsString("/compras/"))
                .body("status", is("CONFIRMADA"))
                .body("sessaoId", is(sessao.id().intValue()))
                .body("assentoId", is(assento.id().intValue()))
                .body("valorCentavos", is(2500))
                .body("ingresso.codigo", notNullValue())
                .body("ingresso.emitidoEm", notNullValue())
                .extract().path("id");

        given()
                .when().get("/compras/{id}", compraId)
                .then()
                .statusCode(200)
                .body("status", is("CONFIRMADA"))
                .body("ingresso.codigo", notNullValue());
    }

    @Test
    void rotaRecusaAssentoJaVendido() {
        comprarPelaRota().statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("assentoId", assento.id()))
                .when().post("/sessoes/{id}/compras", sessao.id())
                .then()
                .statusCode(409)
                .contentType("application/problem+json");
    }

    @Test
    void rotaRecusaAssentoDeOutraSala() {
        prepararAssentoLivre();
        Sala outraSala = dados.sala();
        var assentoDeOutraSala = dados.assento(outraSala, "A", 1);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("assentoId", assentoDeOutraSala.id()))
                .when().post("/sessoes/{id}/compras", sessao.id())
                .then()
                .statusCode(404)
                .contentType("application/problem+json");
    }

    @Test
    void rotaExigeOAssento() {
        prepararAssentoLivre();

        given()
                .contentType(ContentType.JSON)
                .body(Map.of())
                .when().post("/sessoes/{id}/compras", sessao.id())
                .then()
                .statusCode(400)
                .contentType("application/problem+json");
    }

    @Test
    void consultaDeCompraInexistenteDevolve404() {
        given()
                .when().get("/compras/{id}", 999_999)
                .then()
                .statusCode(404)
                .contentType("application/problem+json");
    }

    @Test
    void recuperacaoConfirmaCompraPendenteQuandoOPagamentoEAprovado() {
        Compra compra = compraPendente();

        recuperarTodasAsPendentes();

        assertStatusGravado(compra, StatusDaCompra.CONFIRMADA);
        assertTrue(compras.ingressoDaCompra(compra.id()).isPresent());
        assertAssentoVendido(compra);
    }

    @Test
    void recuperacaoNaoMexeEmCompraAindaDentroDoPrazo() {
        Compra compra = compraPendente();

        compras.recuperarPendentesCriadasAntesDe(OffsetDateTime.now().minusMinutes(10));

        assertStatusGravado(compra, StatusDaCompra.PENDENTE);
        assertSemIngresso(compra);
        assertAssentoReservado(compra);
    }
}
