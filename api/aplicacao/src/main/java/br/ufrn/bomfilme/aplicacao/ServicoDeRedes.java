package br.ufrn.bomfilme.aplicacao;

import br.ufrn.bomfilme.dominio.ErroDeDominio;
import br.ufrn.bomfilme.dominio.RedeCinema;
import br.ufrn.bomfilme.dominio.RepositorioDeCinemas;
import br.ufrn.bomfilme.dominio.RepositorioDeRedes;
import br.ufrn.bomfilme.dominio.Pagina;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/** Casos de uso das redes de cinema. */
@ApplicationScoped
public class ServicoDeRedes {

    @Inject
    RepositorioDeRedes redes;

    @Inject
    RepositorioDeCinemas cinemas;

    public Pagina<RedeCinema> listar(String nome, int pagina, int tamanho) {
        return redes.listar(nome, pagina, tamanho);
    }

    public RedeCinema porId(long id) {
        return redes.porId(id).orElseThrow(() -> naoEncontrada(id));
    }

    public RedeCinema criar(String nome) {
        return redes.criar(nome.trim());
    }

    public RedeCinema atualizar(long id, String nome) {
        return redes.atualizar(id, nome.trim()).orElseThrow(() -> naoEncontrada(id));
    }

    /** Recusa a remoção de uma rede que ainda tem cinemas, em vez de deixar a FK estourar. */
    public void remover(long id) {
        if (redes.porId(id).isEmpty()) {
            throw naoEncontrada(id);
        }
        if (cinemas.existePorRede(id)) {
            throw new ErroDeDominio.Conflito(
                    "rede " + id + " possui cinemas vinculados e não pode ser removida");
        }
        if (!redes.remover(id)) {
            throw naoEncontrada(id);
        }
    }

    private static ErroDeDominio.NaoEncontrado naoEncontrada(long id) {
        return new ErroDeDominio.NaoEncontrado("rede " + id + " não encontrada");
    }
}
