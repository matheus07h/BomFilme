package br.ufrn.bomfilme.adaptadores.pagamentos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.ufrn.bomfilme.dominio.ProcessadorDePagamentos;
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
class PagamentosGrpcRecusaTest {

    @Inject
    ProcessadorDePagamentos pagamentos;

    @Test
    void retornaRecusaComoResultadoDoPagamento() {
        assertEquals(
                ProcessadorDePagamentos.Resultado.RECUSADO,
                pagamentos.processar(1L, 2500L));
    }
}