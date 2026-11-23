package br.ufrn.bomfilme.adaptadores.pagamentos;

import br.ufrn.bomfilme.adaptadores.pagamentos.v1.PagamentoServiceGrpc.PagamentoServiceBlockingStub;
import br.ufrn.bomfilme.adaptadores.pagamentos.v1.ProcessarPagamentoRequest;
import br.ufrn.bomfilme.dominio.ErroDeDominio;
import br.ufrn.bomfilme.dominio.ProcessadorDePagamentos;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.TimeUnit;

@ApplicationScoped
class AdaptadorDePagamentosGrpc implements ProcessadorDePagamentos {

    @GrpcClient("pagamentos")
    PagamentoServiceBlockingStub cliente;

    @Override
    public Resultado processar(long compraId, long valorCentavos) {
        var solicitacao = ProcessarPagamentoRequest.newBuilder()
                .setCompraId(compraId)
                .setValorCentavos(valorCentavos)
                .build();

        try {
            var resposta = cliente.withDeadlineAfter(1, TimeUnit.SECONDS)
                    .processarPagamento(solicitacao);

            return switch (resposta.getResultado()) {
                case RESULTADO_PAGAMENTO_APROVADO -> Resultado.APROVADO;
                case RESULTADO_PAGAMENTO_RECUSADO -> Resultado.RECUSADO;
                default -> throw new ErroDeDominio.ServicoExternoIndisponivel(
                        "O serviço de pagamentos retornou um resultado inválido");
            };
        } catch (StatusRuntimeException erro) {
            if (erro.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                throw new ErroDeDominio.ServicoExternoIndisponivel(
                        "O serviço de pagamentos excedeu o prazo de resposta");
            }

            throw new ErroDeDominio.ServicoExternoIndisponivel(
                    "Falha na comunicação com o serviço de pagamentos");
        }
    }
}