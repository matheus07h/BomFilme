package br.ufrn.bomfilme.adaptadores.pagamentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.ufrn.bomfilme.dominio.ErroDeDominio;
import br.ufrn.bomfilme.dominio.ProcessadorDePagamentos;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(
        value = ServidorGoDeTeste.class,
        initArgs = @ResourceArg(name = "cenario", value = "demora"),
        restrictToAnnotatedClass = true)
class PagamentosGrpcTimeoutTest {

    @Inject
    ProcessadorDePagamentos pagamentos;

    @Test
    void sinalizaQuandoPagamentoExcedeOPrazo() {
        var erro = assertThrows(
                ErroDeDominio.ServicoExternoIndisponivel.class,
                () -> pagamentos.processar(1L, 2500L));

        assertEquals("O serviço de pagamentos excedeu o prazo de resposta", erro.getMessage());
    }
}