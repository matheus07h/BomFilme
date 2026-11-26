package br.ufrn.bomfilme.aplicacao;

import br.ufrn.bomfilme.dominio.Assento;
import br.ufrn.bomfilme.dominio.Compra;
import br.ufrn.bomfilme.dominio.ErroDeDominio;
import br.ufrn.bomfilme.dominio.Ingresso;
import br.ufrn.bomfilme.dominio.ProcessadorDePagamentos;
import br.ufrn.bomfilme.dominio.RepositorioDeAssentos;
import br.ufrn.bomfilme.dominio.RepositorioDeCompras;
import br.ufrn.bomfilme.dominio.RepositorioDeSessoes;
import br.ufrn.bomfilme.dominio.Sessao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.System.Logger.Level;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Compra de ingresso com pagamento simulado pelo serviço Go (issue #8).
 *
 * <p>O caso de uso não é transacional de propósito. A reserva e a decisão sobre o
 * pagamento são duas transações curtas, uma de cada lado da chamada gRPC: segurar uma
 * transação aberta durante a chamada prenderia a conexão do banco pelo tempo da resposta
 * do serviço externo, e uma falha nele desfaria também a reserva.
 */
@ApplicationScoped
public class ServicoDeCompras {

    /**
     * Preço único do ingresso, em centavos.
     *
     * <p>Provisório: as sessões ainda não têm preço. Quando tiverem, o valor sai daí.
     */
    static final long PRECO_DO_INGRESSO_CENTAVOS = 2_500;

    private static final System.Logger LOG = System.getLogger(ServicoDeCompras.class.getName());

    @Inject
    RepositorioDeCompras compras;

    @Inject
    RepositorioDeSessoes sessoes;

    @Inject
    RepositorioDeAssentos assentos;

    @Inject
    ProcessadorDePagamentos pagamentos;

    /**
     * Reserva o assento, pede o pagamento e devolve a compra no estado em que ficou.
     *
     * <ul>
     *   <li>Aprovação: a compra e a reserva são confirmadas e o ingresso é emitido.</li>
     *   <li>Recusa: a compra é recusada, a reserva é liberada e não há ingresso.</li>
     *   <li>Timeout ou falha de comunicação: a compra continua pendente, com o assento
     *       reservado e sem ingresso. Sem resposta não sabemos se houve cobrança, então
     *       nem confirmamos nem liberamos o assento na hora: quem decide é
     *       {@link #recuperarPendentesCriadasAntesDe}.</li>
     * </ul>
     */
    public Compra comprar(long sessaoId, long assentoId) {
        Sessao sessao = sessoes.porId(sessaoId)
                .orElseThrow(() -> ErroDeDominio.NaoEncontrado.de("sessão", sessaoId));
        Assento assento = assentos.porId(assentoId)
                .filter(candidato -> candidato.salaId().equals(sessao.salaId()))
                .orElseThrow(() -> new ErroDeDominio.NaoEncontrado(
                        "assento " + assentoId + " não pertence a esta sessão"));

        // A unicidade no banco é a garantia; esta checagem só dá uma mensagem melhor.
        if (assentos.estaOcupado(sessao.id(), assento.id())) {
            throw new ErroDeDominio.Conflito(
                    "assento " + assentoId + " já está ocupado nesta sessão");
        }
        Compra compra = compras.reservar(
                sessao.id(), assento.id(), UUID.randomUUID(), PRECO_DO_INGRESSO_CENTAVOS);

        ProcessadorDePagamentos.Resultado resultado;
        try {
            resultado = pagamentos.processar(compra.id(), compra.valorCentavos());
        } catch (ErroDeDominio.ServicoExternoIndisponivel erro) {
            LOG.log(Level.WARNING, "compra {0} continua pendente: {1}", compra.id(), erro.getMessage());
            return compra;
        }

        aplicar(compra, resultado);
        return porId(compra.id());
    }

    /**
     * Decide as compras que ficaram pendentes e foram criadas antes de {@code limite}.
     *
     * <p>Para cada uma, pede o pagamento de novo ao serviço Go, que identifica a compra
     * pelo id. Aprovação confirma e emite o ingresso; recusa libera o assento; sem
     * resposta outra vez, a compra expira e o assento é liberado. Assim nenhum assento
     * fica preso para sempre, e nenhuma compra é confirmada sem uma aprovação.
     *
     * <p>Uma compra que falhe não interrompe as outras. Se duas instâncias recuperarem a
     * mesma compra ao mesmo tempo, a troca de status condicional deixa só uma decidir.
     *
     * @return quantas compras foram decididas
     */
    public int recuperarPendentesCriadasAntesDe(OffsetDateTime limite) {
        int decididas = 0;
        for (Compra compra : compras.pendentesCriadasAntesDe(limite)) {
            try {
                if (recuperar(compra)) {
                    decididas++;
                }
            } catch (RuntimeException erro) {
                LOG.log(Level.ERROR, "falha ao recuperar a compra " + compra.id(), erro);
            }
        }
        return decididas;
    }

    private boolean recuperar(Compra compra) {
        ProcessadorDePagamentos.Resultado resultado;
        try {
            resultado = pagamentos.processar(compra.id(), compra.valorCentavos());
        } catch (ErroDeDominio.ServicoExternoIndisponivel erro) {
            LOG.log(Level.WARNING, "compra {0} expirou sem resposta do pagamento: {1}",
                    compra.id(), erro.getMessage());
            return compras.expirar(compra.id());
        }
        return aplicar(compra, resultado);
    }

    private boolean aplicar(Compra compra, ProcessadorDePagamentos.Resultado resultado) {
        return switch (resultado) {
            case APROVADO -> compras.confirmar(compra.id()).isPresent();
            case RECUSADO -> compras.recusar(compra.id());
        };
    }

    public Compra porId(long id) {
        return compras.porId(id).orElseThrow(() -> new ErroDeDominio.NaoEncontrado(
                "compra " + id + " não encontrada"));
    }

    public Optional<Ingresso> ingressoDaCompra(long compraId) {
        return compras.ingressoDaCompra(compraId);
    }
}
