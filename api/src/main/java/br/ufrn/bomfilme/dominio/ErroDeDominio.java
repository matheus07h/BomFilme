package br.ufrn.bomfilme.dominio;

/**
 * Falhas que o domínio e os casos de uso sinalizam sem saber de HTTP.
 *
 * <p>A tradução para status e corpo de resposta é trabalho do adaptador HTTP: é lá que
 * {@code NaoEncontrado} vira 404 e {@code Conflito} vira 409. Manter as exceções aqui é
 * o que permite ao domínio recusar uma operação sem conhecer a camada web.
 */
public abstract sealed class ErroDeDominio extends RuntimeException {

    protected ErroDeDominio(String mensagem) {
        super(mensagem);
    }

    /** O registro pedido não existe. */
    public static final class NaoEncontrado extends ErroDeDominio {
        public NaoEncontrado(String mensagem) {
            super(mensagem);
        }

        public static NaoEncontrado de(String recurso, long id) {
            return new NaoEncontrado(recurso + " " + id + " não encontrado");
        }
    }

    /** A operação conflita com o estado atual: vínculo existente, duplicidade, horário ocupado. */
    public static final class Conflito extends ErroDeDominio {
        public Conflito(String mensagem) {
            super(mensagem);
        }
    }

    /** A entrada é bem formada, mas uma regra de negócio recusa a operação. */
    public static final class RegraViolada extends ErroDeDominio {
        public RegraViolada(String mensagem) {
            super(mensagem);
        }
    }

    /** Um serviço externo de que dependemos falhou ou está fora. */
    public static final class ServicoExternoIndisponivel extends ErroDeDominio {
        public ServicoExternoIndisponivel(String mensagem) {
            super(mensagem);
        }
    }
}
