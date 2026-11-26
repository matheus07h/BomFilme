package br.ufrn.bomfilme.compras;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufrn.bomfilme.adaptadores.pagamentos.ServidorGoDeTeste;
import br.ufrn.bomfilme.dominio.Compra;
import br.ufrn.bomfilme.dominio.StatusDaCompra;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(
        value = ServidorGoDeTeste.class,
        initArgs = @ResourceArg(name = "cenario", value = "recusa"),
        restrictToAnnotatedClass = true)
class CompraRecusadaTest extends CenarioDeCompra {

    @Test
    void recusaNaoConfirmaNaoEmiteIngressoELiberaOAssento() {
        Compra compra = comprarAssentoLivre();

        assertEquals(StatusDaCompra.RECUSADA, compra.status());
        assertStatusGravado(compra, StatusDaCompra.RECUSADA);
        assertSemIngresso(compra);
        assertAssentoLivre(compra);
    }

    @Test
    void compraRecusadaNaoPodeSerConfirmadaDepois() {
        Compra compra = comprarAssentoLivre();

        assertTrue(repositorio.confirmar(compra.id()).isEmpty());
        assertStatusGravado(compra, StatusDaCompra.RECUSADA);
        assertSemIngresso(compra);
    }

    @Test
    void rotaDevolveACompraRecusadaSemIngresso() {
        comprarPelaRota()
                .statusCode(201)
                .body("status", is("RECUSADA"))
                .body("ingresso", nullValue());
    }

    @Test
    void recuperacaoRecusaCompraPendenteELiberaOAssento() {
        Compra compra = compraPendente();

        recuperarTodasAsPendentes();

        assertStatusGravado(compra, StatusDaCompra.RECUSADA);
        assertSemIngresso(compra);
        assertAssentoLivre(compra);
    }
}
