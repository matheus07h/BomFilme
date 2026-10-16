package br.ufrn.bomfilme;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Traduz NotFoundException para o formato padrão de erro da API. */
@Provider
public class MapeadorDeNotFound implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException excecao) {
        String mensagem = excecao.getMessage() != null ? excecao.getMessage() : "recurso não encontrado";
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(new Erro(mensagem))
                .build();
    }
}