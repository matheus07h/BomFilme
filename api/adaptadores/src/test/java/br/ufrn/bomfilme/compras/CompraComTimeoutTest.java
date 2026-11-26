package br.ufrn.bomfilme.compras;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.ufrn.bomfilme.adaptadores.pagamentos.ServidorGoDeTeste;
import br.ufrn.bomfilme.dominio.Compra;
import br.ufrn.bomfilme.dominio.ErroDeDominio;
import br.ufrn.bomfilme.dominio.StatusDaCompra;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** O serviço Go demora 3s e o cliente desiste em 1s. */
@QuarkusTest
@QuarkusTestResource(
        value = ServidorGoDeTeste.class,
        initArgs = @ResourceArg(name = "cenario", value = "demora"),
        restrictToAnnotatedClass = true)
class CompraComTimeoutTest extends CenarioDeCompra {

    @Test
    void timeoutDeixaACompraPendenteESemIngresso() {
        Compra compra = comprarAssentoLivre();

        assertEquals(StatusDaCompra.PENDENTE, compra.status());
        assertStatusGravado(compra, StatusDaCompra.PENDENTE);
        assertSemIngresso(compra);
        assertAssentoReservado(compra);
    }

    @Test
    void compraPendenteSeguraOAssentoParaOutrasCompras() {
        comprarAssentoLivre();

        assertThrows(ErroDeDominio.Conflito.class,
                () -> compras.comprar(sessao.id(), assento.id()));
    }

    @Test
    void rotaDevolveACompraPendenteSemIngresso() {
        comprarPelaRota()
                .statusCode(202)
                .header("Location", containsString("/compras/"))
                .body("status", is("PENDENTE"))
                .body("ingresso", nullValue());
    }

    @Test
    void recuperacaoSemRespostaExpiraACompraELiberaOAssento() {
        Compra compra = comprarAssentoLivre();

        recuperarTodasAsPendentes();

        assertStatusGravado(compra, StatusDaCompra.EXPIRADA);
        assertSemIngresso(compra);
        assertAssentoLivre(compra);
    }

    @Test
    void assentoDeCompraExpiradaPodeSerCompradoDeNovo() {
        Compra expirada = comprarAssentoLivre();
        recuperarTodasAsPendentes();

        Compra nova = compras.comprar(sessao.id(), assento.id());

        assertStatusGravado(expirada, StatusDaCompra.EXPIRADA);
        assertEquals(StatusDaCompra.PENDENTE, nova.status());
        assertAssentoReservado(nova);
    }
}
