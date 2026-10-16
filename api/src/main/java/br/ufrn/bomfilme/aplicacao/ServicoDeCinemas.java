package br.ufrn.bomfilme.aplicacao;

import br.ufrn.bomfilme.dominio.Cinema;
import br.ufrn.bomfilme.dominio.ErroDeDominio;
import br.ufrn.bomfilme.dominio.FiltroDeCinemas;
import br.ufrn.bomfilme.dominio.RepositorioDeCinemas;
import br.ufrn.bomfilme.dominio.RepositorioDeRedes;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Locale;

/** Casos de uso dos cinemas. */
@ApplicationScoped
public class ServicoDeCinemas {

    @Inject
    RepositorioDeCinemas cinemas;

    @Inject
    RepositorioDeRedes redes;

    public List<Cinema> listar(FiltroDeCinemas filtro, int pagina, int tamanho) {
        return cinemas.listar(filtro, pagina, tamanho);
    }

    public Cinema porId(long id) {
        return cinemas.porId(id).orElseThrow(() -> naoEncontrado(id));
    }

    public Cinema criar(long redeId, String nome, String cidade, String uf) {
        exigirRede(redeId);
        return cinemas.criar(redeId, nome.trim(), cidade.trim(), normalizarUf(uf));
    }

    public Cinema atualizar(long id, long redeId, String nome, String cidade, String uf) {
        if (cinemas.porId(id).isEmpty()) {
            throw naoEncontrado(id);
        }
        exigirRede(redeId);
        return cinemas.atualizar(id, redeId, nome.trim(), cidade.trim(), normalizarUf(uf))
                .orElseThrow(() -> naoEncontrado(id));
    }

    /** Recusa a remoção de um cinema que ainda tem salas, em vez de deixar a FK estourar. */
    public void remover(long id) {
        if (cinemas.porId(id).isEmpty()) {
            throw naoEncontrado(id);
        }
        if (cinemas.possuiSalas(id)) {
            throw new ErroDeDominio.Conflito(
                    "cinema " + id + " possui salas vinculadas e não pode ser removido");
        }
        if (!cinemas.remover(id)) {
            throw naoEncontrado(id);
        }
    }

    private void exigirRede(long redeId) {
        if (redes.porId(redeId).isEmpty()) {
            throw new ErroDeDominio.NaoEncontrado("rede " + redeId + " não encontrada");
        }
    }

    private static String normalizarUf(String uf) {
        return uf.toUpperCase(Locale.ROOT);
    }

    private static ErroDeDominio.NaoEncontrado naoEncontrado(long id) {
        return new ErroDeDominio.NaoEncontrado("cinema " + id + " não encontrado");
    }
}
