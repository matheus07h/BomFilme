package br.ufrn.bomfilme.dominio;

/** Porta de saída usada para solicitar o pagamento de uma compra. */
public interface ProcessadorDePagamentos {

    enum Resultado {
        APROVADO,
        RECUSADO
    }

    Resultado processar(long compraId, long valorCentavos);
}