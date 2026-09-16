package br.ufrn.bomfilme;

import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

/**
 * Traduz violações de unicidade do banco em 409 Conflict.
 *
 * A regra de unicidade fica no próprio banco (restrições da migração V1), o que
 * também cobre duas requisições simultâneas tentando criar o mesmo registro.
 * Qualquer outro erro de persistência continua sendo 500, mas com log e sem
 * expor detalhes internos ao cliente.
 */
@Provider
public class MapeadorDeErrosDePersistencia implements ExceptionMapper<PersistenceException> {

    private static final Logger LOG = Logger.getLogger(MapeadorDeErrosDePersistencia.class);

    /** Código SQLSTATE do PostgreSQL para unique_violation. */
    static final String VIOLACAO_DE_UNICIDADE = "23505";

    @Override
    public Response toResponse(PersistenceException excecao) {
        ConstraintViolationException violacao = procurarViolacao(excecao);

        if (violacao != null && VIOLACAO_DE_UNICIDADE.equals(violacao.getSQLState())) {
            return resposta(Response.Status.CONFLICT, "já existe um registro com esses dados");
        }

        LOG.error("Erro de persistência não tratado", excecao);
        return resposta(Response.Status.INTERNAL_SERVER_ERROR, "erro interno ao acessar o banco de dados");
    }

    private static ConstraintViolationException procurarViolacao(Throwable erro) {
        Throwable atual = erro;
        while (atual != null) {
            if (atual instanceof ConstraintViolationException violacao) {
                return violacao;
            }
            if (atual.getCause() == atual) {
                break;
            }
            atual = atual.getCause();
        }
        return null;
    }

    private static Response resposta(Response.Status status, String mensagem) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new Erro(mensagem))
                .build();
    }
}
