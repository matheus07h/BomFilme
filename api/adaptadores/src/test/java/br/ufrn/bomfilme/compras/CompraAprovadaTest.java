package br.ufrn.bomfilme.compras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufrn.bomfilme.adaptadores.pagamentos.ServidorGoDeTeste;
import br.ufrn.bomfilme.dominio.Compra;
import br.ufrn.bomfilme.dominio.ErroDeDominio;
import br.ufrn.bomfilme.dominio.Ingresso;
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
        initArgs = @ResourceArg(name = "cenario", value = "aprovacao"),
        restrictToAnnotatedClass = true)
class CompraAprovadaTest extends CenarioDeCompra {

    @Inject
    RepositorioDeCompras repositorio;

    @Test
    void aprovacaoConfirmaACompraEEmiteOIngresso() {
        Compra compra = comprarAssentoLivre();

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
}
