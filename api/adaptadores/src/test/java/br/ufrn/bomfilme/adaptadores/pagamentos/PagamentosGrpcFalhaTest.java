package br.ufrn.bomfilme.adaptadores.pagamentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.ufrn.bomfilme.adaptadores.pagamentos.v1.PagamentoServiceGrpc.PagamentoServiceBlockingStub;
import br.ufrn.bomfilme.adaptadores.pagamentos.v1.ProcessarPagamentoRequest;
import br.ufrn.bomfilme.dominio.ErroDeDominio;
import br.ufrn.bomfilme.dominio.ProcessadorDePagamentos;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(
        value = ServidorGoDeTeste.class,
        initArgs = @ResourceArg(name = "cenario", value = "falha"),
        restrictToAnnotatedClass = true)
class PagamentosGrpcFalhaTest {

    @GrpcClient("pagamentos")
    PagamentoServiceBlockingStub cliente;

    @Inject
    ProcessadorDePagamentos pagamentos;

    @Test
    void sinalizaQuandoServicoEstaIndisponivel() {
        var solicitacao = ProcessarPagamentoRequest.newBuilder()
                .setCompraId(1L)
                .setValorCentavos(2500L)
                .build();

        var erroGrpc = assertThrows(StatusRuntimeException.class,
                () -> cliente.withDeadlineAfter(3, TimeUnit.SECONDS)
                        .processarPagamento(solicitacao));

        assertEquals(Status.Code.UNAVAILABLE, erroGrpc.getStatus().getCode());
        assertEquals("indisponibilidade simulada", erroGrpc.getStatus().getDescription());

        var erroDeDominio = assertThrows(ErroDeDominio.ServicoExternoIndisponivel.class,
                () -> pagamentos.processar(1L, 2500L));

        assertEquals("Falha na comunicação com o serviço de pagamentos", erroDeDominio.getMessage());
    }
}
