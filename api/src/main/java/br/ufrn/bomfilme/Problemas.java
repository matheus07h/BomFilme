package br.ufrn.bomfilme;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Fábricas das respostas de erro da API. Concentrar os {@code type} aqui evita que a
 * mesma classe de erro ganhe URNs diferentes em cada recurso.
 */
public final class Problemas {

    /** Status para entrada bem formada que não pode ser processada (RFC 9110, seção 15.5.21). */
    public static final int ENTIDADE_NAO_PROCESSAVEL = 422;

    private static final String PREFIXO = "urn:bomfilme:erro:";

    private Problemas() {
    }

    public static Response naoEncontrado(String detalhe) {
        return resposta(Response.Status.NOT_FOUND.getStatusCode(),
                "nao-encontrado", "Recurso não encontrado", detalhe);
    }

    public static Response conflito(String detalhe) {
        return resposta(Response.Status.CONFLICT.getStatusCode(),
                "conflito", "Conflito com o estado atual", detalhe);
    }

    public static Response naoProcessavel(String detalhe) {
        return resposta(ENTIDADE_NAO_PROCESSAVEL,
                "nao-processavel", "Entidade não processável", detalhe);
    }

    public static Response entradaInvalida(String detalhe, List<String> violacoes) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(Problema.MEDIA_TYPE)
                .entity(new Problema(PREFIXO + "entrada-invalida", "Entrada inválida",
                        Response.Status.BAD_REQUEST.getStatusCode(), detalhe, violacoes))
                .build();
    }

    public static Response erroInterno(String detalhe) {
        return resposta(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "erro-interno", "Erro interno", detalhe);
    }

    public static Response servicoIndisponivel(String detalhe) {
        return resposta(Response.Status.BAD_GATEWAY.getStatusCode(),
                "servico-externo-indisponivel", "Serviço externo indisponível", detalhe);
    }

    /** Versão lançável, para quando o erro nasce fora da camada de recurso. */
    public static WebApplicationException comoExcecao(Response resposta) {
        return new WebApplicationException(resposta);
    }

    private static Response resposta(int status, String tipo, String titulo, String detalhe) {
        return Response.status(status)
                .type(Problema.MEDIA_TYPE)
                .entity(new Problema(PREFIXO + tipo, titulo, status, detalhe))
                .build();
    }
}
