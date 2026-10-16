package br.ufrn.bomfilme.adaptadores.http;

import br.ufrn.bomfilme.dominio.ErroDeDominio;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduz as falhas do domínio em status HTTP e <em>problem details</em>.
 *
 * <p>Este mapeador é o único ponto onde o vocabulário do domínio encontra o do HTTP, e é
 * o que permite aos casos de uso recusar uma operação sem conhecer códigos de status.
 */
@Provider
public class MapeadorDeErroDeDominio implements ExceptionMapper<ErroDeDominio> {

    @Override
    public Response toResponse(ErroDeDominio erro) {
        return switch (erro) {
            case ErroDeDominio.NaoEncontrado e -> Problemas.naoEncontrado(e.getMessage());
            case ErroDeDominio.Conflito e -> Problemas.conflito(e.getMessage());
            case ErroDeDominio.RegraViolada e -> Problemas.naoProcessavel(e.getMessage());
            case ErroDeDominio.ServicoExternoIndisponivel e -> Problemas.servicoIndisponivel(e.getMessage());
            case ErroDeDominio.FalhaInesperada e -> Problemas.erroInterno(e.getMessage());
        };
    }
}
