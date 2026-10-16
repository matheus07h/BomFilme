package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.ErroDeDominio;
import jakarta.persistence.PersistenceException;
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
     * Devolve o erro de domínio correspondente, ou repropaga quando não é violação de
     * restrição — um erro de persistência que não sabemos classificar não deve virar 409.
     */
    private static RuntimeException traduzir(PersistenceException excecao) {
        ConstraintViolationException violacao = procurarViolacao(excecao);
        if (violacao == null) {
            return excecao;
        }
        return switch (violacao.getSQLState()) {
            case UNICIDADE -> new ErroDeDominio.Conflito("já existe um registro com esses dados");
            case CHAVE_ESTRANGEIRA -> new ErroDeDominio.Conflito("operação conflita com registros vinculados");
            case EXCLUSAO -> new ErroDeDominio.Conflito("a sala já possui uma sessão nesse horário");
            case null, default -> excecao;
        };
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
