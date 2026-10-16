package br.ufrn.bomfilme.aplicacao;

import br.ufrn.bomfilme.dominio.Assento;
import br.ufrn.bomfilme.dominio.ErroDeDominio;
import br.ufrn.bomfilme.dominio.FiltroDeSalas;
import br.ufrn.bomfilme.dominio.RepositorioDeAssentos;
import br.ufrn.bomfilme.dominio.RepositorioDeCinemas;
import br.ufrn.bomfilme.dominio.RepositorioDeSalas;
import br.ufrn.bomfilme.dominio.Sala;
import br.ufrn.bomfilme.dominio.Pagina;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Casos de uso das salas e dos assentos que pertencem a elas. */
@ApplicationScoped
public class ServicoDeSalas {

    @Inject
    RepositorioDeSalas salas;

    @Inject
    RepositorioDeCinemas cinemas;

    @Inject
    RepositorioDeAssentos assentos;

    public Pagina<Sala> listar(FiltroDeSalas filtro, int pagina, int tamanho) {
        return salas.listar(filtro, pagina, tamanho);
    }

    public Sala porId(long id) {
        return salas.porId(id).orElseThrow(() -> salaNaoEncontrada(id));
    }

    public Sala criar(long cinemaId, String nome) {
        exigirCinema(cinemaId);
        return salas.criar(cinemaId, nome.trim());
    }

    public Sala atualizar(long id, long cinemaId, String nome) {
        if (salas.porId(id).isEmpty()) {
            throw salaNaoEncontrada(id);
        }
        exigirCinema(cinemaId);
        return salas.atualizar(id, cinemaId, nome.trim()).orElseThrow(() -> salaNaoEncontrada(id));
    }

    public void remover(long id) {
        if (salas.porId(id).isEmpty()) {
            throw salaNaoEncontrada(id);
        }
        if (salas.possuiSessoes(id)) {
            throw new ErroDeDominio.Conflito(
                    "sala " + id + " possui sessões vinculadas e não pode ser removida");
        }
        if (salas.possuiAssentos(id)) {
            throw new ErroDeDominio.Conflito(
                    "sala " + id + " possui assentos vinculados e não pode ser removida");
        }
        if (!salas.remover(id)) {
            throw salaNaoEncontrada(id);
        }
    }

    public Pagina<Assento> listarAssentos(long salaId, String fileira, int pagina, int tamanho) {
        if (salas.porId(salaId).isEmpty()) {
            throw salaNaoEncontrada(salaId);
        }
        return assentos.listarDaSala(salaId, normalizarFileira(fileira), pagina, tamanho);
    }

    public Assento assentoPorId(long salaId, long id) {
        return assentoDaSala(salaId, id).orElseThrow(() -> assentoNaoEncontrado(salaId, id));
    }

    public Assento criarAssento(long salaId, String fileira, int numero) {
        if (salas.porId(salaId).isEmpty()) {
            throw salaNaoEncontrada(salaId);
        }
        return assentos.criar(salaId, normalizarFileira(fileira), numero);
    }

    public Assento atualizarAssento(long salaId, long id, String fileira, int numero) {
        if (assentoDaSala(salaId, id).isEmpty()) {
            throw assentoNaoEncontrado(salaId, id);
        }
        return assentos.atualizar(id, normalizarFileira(fileira), numero)
                .orElseThrow(() -> assentoNaoEncontrado(salaId, id));
    }

    public void removerAssento(long salaId, long id) {
        if (assentoDaSala(salaId, id).isEmpty()) {
            throw assentoNaoEncontrado(salaId, id);
        }
        if (assentos.possuiOcupacoes(id)) {
            throw new ErroDeDominio.Conflito(
                    "assento " + id + " está ocupado em alguma sessão e não pode ser removido");
        }
        if (!assentos.remover(id)) {
            throw assentoNaoEncontrado(salaId, id);
        }
    }

    private Optional<Assento> assentoDaSala(long salaId, long id) {
        return assentos.porId(id).filter(assento -> assento.salaId() == salaId);
    }

    private void exigirCinema(long cinemaId) {
        if (cinemas.porId(cinemaId).isEmpty()) {
            throw new ErroDeDominio.NaoEncontrado("cinema " + cinemaId + " não encontrado");
        }
    }

    private static String normalizarFileira(String fileira) {
        return fileira == null ? null : fileira.toUpperCase(Locale.ROOT);
    }

    private static ErroDeDominio.NaoEncontrado salaNaoEncontrada(long id) {
        return new ErroDeDominio.NaoEncontrado("sala " + id + " não encontrada");
    }

    private static ErroDeDominio.NaoEncontrado assentoNaoEncontrado(long salaId, long id) {
        return new ErroDeDominio.NaoEncontrado("assento " + id + " não encontrado na sala " + salaId);
    }
}
