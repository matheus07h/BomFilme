package br.ufrn.bomfilme;

import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

/**
 * Traduz violações de unicidade e de chave estrangeira do banco em 409 Conflict.
 *
 * A regra de unicidade fica no próprio banco (restrições da migração V1), o que
 * também cobre duas requisições simultâneas tentando criar o mesmo registro. As
 * chaves estrangeiras, por sua vez, impedem remover um registro que ainda tem vínculos.
 * Qualquer outro erro de persistência continua sendo 500, mas com log e sem
 * expor detalhes internos ao cliente.
 */
@Provider
public class MapeadorDeErrosDePersistencia implements ExceptionMapper<PersistenceException> {

    private static final Logger LOG = Logger.getLogger(MapeadorDeErrosDePersistencia.class);

    /** Código SQLSTATE do PostgreSQL para unique_violation. */
    static final String VIOLACAO_DE_UNICIDADE = "23505";

    /** Código SQLSTATE do PostgreSQL para foreign_key_violation. */
    static final String VIOLACAO_DE_CHAVE_ESTRANGEIRA = "23503";

    /** Código SQLSTATE do PostgreSQL para exclusion_violation (sessões sobrepostas na mesma sala). */
    static final String VIOLACAO_DE_EXCLUSAO = "23P01";

    @Override
    public Response toResponse(PersistenceException excecao) {
        ConstraintViolationException violacao = procurarViolacao(excecao);

        if (violacao != null && VIOLACAO_DE_UNICIDADE.equals(violacao.getSQLState())) {
            return Problemas.conflito("já existe um registro com esses dados");
        }
        if (violacao != null && VIOLACAO_DE_CHAVE_ESTRANGEIRA.equals(violacao.getSQLState())) {
            return Problemas.conflito("operação conflita com registros vinculados");
        }
        if (violacao != null && VIOLACAO_DE_EXCLUSAO.equals(violacao.getSQLState())) {
            return Problemas.conflito("a sala já possui uma sessão nesse horário");
        }

        LOG.error("Erro de persistência não tratado", excecao);
        return Problemas.erroInterno("erro interno ao acessar o banco de dados");
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

}
