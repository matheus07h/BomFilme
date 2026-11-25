package br.ufrn.bomfilme.compras;

import br.ufrn.bomfilme.adaptadores.pagamentos.ServidorGoDeTeste;
import br.ufrn.bomfilme.dominio.Compra;
import br.ufrn.bomfilme.dominio.StatusDaCompra;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** O serviço Go responde UNAVAILABLE a toda solicitação. */
@QuarkusTest
@QuarkusTestResource(
        value = ServidorGoDeTeste.class,
        initArgs = @ResourceArg(name = "cenario", value = "falha"),
        restrictToAnnotatedClass = true)
class CompraComServicoIndisponivelTest extends CenarioDeCompra {

    @Test
    void indisponibilidadeDeixaACompraPendenteESemIngresso() {
        Compra compra = comprarAssentoLivre();

        assertStatusGravado(compra, StatusDaCompra.PENDENTE);
        assertSemIngresso(compra);
        assertAssentoReservado(compra);
    }
}
