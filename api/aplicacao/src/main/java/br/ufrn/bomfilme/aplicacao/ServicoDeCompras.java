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
     *       nem confirmamos nem liberamos o assento automaticamente.</li>
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

        switch (resultado) {
            case APROVADO -> compras.confirmar(compra.id());
            case RECUSADO -> compras.recusar(compra.id());
        }
        return porId(compra.id());
    }

    public Compra porId(long id) {
        return compras.porId(id).orElseThrow(() -> new ErroDeDominio.NaoEncontrado(
                "compra " + id + " não encontrada"));
    }

    public Optional<Ingresso> ingressoDaCompra(long compraId) {
        return compras.ingressoDaCompra(compraId);
    }
}
