package br.ufrn.bomfilme.adaptadores.pagamentos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.ufrn.bomfilme.dominio.ProcessadorDePagamentos;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@QuarkusTest
@QuarkusTestResource(
        value = ServidorGoDeTeste.class,
        restrictToAnnotatedClass = true)
public class PagamentosGrpcLocalTest {

    @Inject
    ProcessadorDePagamentos pagamentos;

    @Test
    void retornaAprovacaoDoServicoGo() {
        assertEquals(
                ProcessadorDePagamentos.Resultado.APROVADO,
                pagamentos.processar(1L, 2500L));
    }

}