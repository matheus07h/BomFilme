package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.ErroDeDominio;
import jakarta.persistence.PersistenceException;
import org.jboss.logging.Logger;
import java.util.function.Supplier;
import org.hibernate.exception.ConstraintViolationException;

/**
 * Traduz as restrições do banco para o vocabulário do domínio.
 *
 * <p>As regras de unicidade, de chave estrangeira e de sobreposição de horários ficam no
 * próprio banco, o que também cobre requisições simultâneas — uma verificação feita só na
 * aplicação não garante isso. A tradução acontece aqui, e não num {@code ExceptionMapper},
 * porque reconhecer um SQLSTATE é conhecimento de persistência: deixá-la na camada web
 * obrigaria o adaptador HTTP a importar JPA e Hibernate.
 */
final class ViolacoesDoBanco {

    private static final Logger LOG = Logger.getLogger(ViolacoesDoBanco.class);

    /** Código SQLSTATE do PostgreSQL para unique_violation. */
    static final String UNICIDADE = "23505";

    /** Código SQLSTATE do PostgreSQL para foreign_key_violation. */
    static final String CHAVE_ESTRANGEIRA = "23503";

    /** Código SQLSTATE do PostgreSQL para exclusion_violation (sessões sobrepostas na sala). */
    static final String EXCLUSAO = "23P01";

    private ViolacoesDoBanco() {
    }

    /** Executa a gravação, convertendo violação de restrição em conflito de domínio. */
    static <T> T gravando(Supplier<T> acao) {
        try {
            return acao.get();
        } catch (PersistenceException excecao) {
            throw traduzir(excecao);
        }
    }

    static void gravando(Runnable acao) {
        gravando(() -> {
            acao.run();
            return null;
        });
    }

    /**
     * Devolve o erro de domínio correspondente à violação.
     *
     * <p>Uma falha de persistência que não sabemos classificar não vira 409: vira
     * {@link ErroDeDominio.FalhaInesperada}, que o adaptador HTTP responde como 500 em
     * <em>problem details</em>. Repropagar a exceção crua faria a resposta cair no
     * tratamento genérico do servidor e sair fora do formato padrão de erro — por isso
     * a causa fica no log, e não na resposta.
     */
    private static RuntimeException traduzir(PersistenceException excecao) {
        ConstraintViolationException violacao = procurarViolacao(excecao);
        String sqlState = violacao == null ? null : violacao.getSQLState();
        return switch (sqlState) {
            case UNICIDADE -> new ErroDeDominio.Conflito("já existe um registro com esses dados");
            case CHAVE_ESTRANGEIRA -> new ErroDeDominio.Conflito("operação conflita com registros vinculados");
            case EXCLUSAO -> new ErroDeDominio.Conflito("a sala já possui uma sessão nesse horário");
            case null, default -> naoClassificada(excecao);
        };
    }

    private static ErroDeDominio.FalhaInesperada naoClassificada(PersistenceException excecao) {
        LOG.error("Erro de persistência não tratado", excecao);
        return new ErroDeDominio.FalhaInesperada("erro interno ao acessar o banco de dados", excecao);
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
