package br.ufrn.bomfilme.compras;

import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufrn.bomfilme.adaptadores.pagamentos.ServidorGoDeTeste;
import br.ufrn.bomfilme.dominio.Compra;
import br.ufrn.bomfilme.dominio.RepositorioDeCompras;
import br.ufrn.bomfilme.dominio.StatusDaCompra;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(
        value = ServidorGoDeTeste.class,
        initArgs = @ResourceArg(name = "cenario", value = "recusa"),
        restrictToAnnotatedClass = true)
class CompraRecusadaTest extends CenarioDeCompra {

    @Inject
    RepositorioDeCompras repositorio;

    @Test
    void recusaNaoConfirmaNaoEmiteIngressoELiberaOAssento() {
        Compra compra = comprarAssentoLivre();

        assertStatusGravado(compra, StatusDaCompra.RECUSADA);
        assertSemIngresso(compra);
        assertAssentoLivre(compra);
    }

    @Test
    void compraRecusadaNaoPodeSerConfirmadaDepois() {
        Compra compra = comprarAssentoLivre();

        assertTrue(repositorio.confirmar(compra.id()).isEmpty());
        assertStatusGravado(compras.porId(compra.id()), StatusDaCompra.RECUSADA);
        assertSemIngresso(compra);
    }
}
