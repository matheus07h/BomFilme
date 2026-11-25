package br.ufrn.bomfilme.compras;

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
}
