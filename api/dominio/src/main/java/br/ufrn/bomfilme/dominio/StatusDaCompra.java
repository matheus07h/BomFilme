package br.ufrn.bomfilme.dominio;

/** Em que pé está uma compra. Só {@link #CONFIRMADA} tem ingresso. */
public enum StatusDaCompra {

    /**
     * O assento está reservado e o pagamento ainda não foi decidido.
     *
     * <p>É também o estado em que a compra fica quando o serviço de pagamentos não
     * responde no prazo ou está fora: sem resposta, não sabemos se houve cobrança, então
     * a compra não é confirmada nem recusada automaticamente.
     */
    PENDENTE,

    /** O pagamento foi aprovado e o ingresso foi emitido. */
    CONFIRMADA,

    /** O pagamento foi recusado e a reserva do assento foi liberada. */
    RECUSADA
}
