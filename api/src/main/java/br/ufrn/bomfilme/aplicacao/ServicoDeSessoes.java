package br.ufrn.bomfilme.aplicacao;

import br.ufrn.bomfilme.dominio.Assento;
import br.ufrn.bomfilme.dominio.AssentoDisponibilidade;
import br.ufrn.bomfilme.dominio.ErroDeDominio;
import br.ufrn.bomfilme.dominio.Filme;
import br.ufrn.bomfilme.dominio.FiltroDeSessoes;
import br.ufrn.bomfilme.dominio.RepositorioDeAssentos;
import br.ufrn.bomfilme.dominio.RepositorioDeFilmes;
import br.ufrn.bomfilme.dominio.RepositorioDeSalas;
import br.ufrn.bomfilme.dominio.RepositorioDeSessoes;
import br.ufrn.bomfilme.dominio.Sessao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/** Casos de uso da programação de sessões e da ocupação de assentos. */
@ApplicationScoped
public class ServicoDeSessoes {

    @Inject
    RepositorioDeSessoes sessoes;

    @Inject
    RepositorioDeSalas salas;

    @Inject
    RepositorioDeFilmes filmes;

    @Inject
    RepositorioDeAssentos assentos;

    public List<Sessao> listar(FiltroDeSessoes filtro, int pagina, int tamanho) {
        return sessoes.listar(filtro, pagina, tamanho);
    }

    public Sessao porId(long id) {
        return sessoes.porId(id).orElseThrow(() -> naoEncontrada(id));
    }

    public Sessao criar(long salaId, long filmeId, OffsetDateTime inicio) {
        Horario horario = planejar(salaId, filmeId, inicio, null);
        return sessoes.criar(salaId, filmeId, horario.inicio(), horario.fim());
    }

    public Sessao atualizar(long id, long salaId, long filmeId, OffsetDateTime inicio) {
        if (sessoes.porId(id).isEmpty()) {
            throw naoEncontrada(id);
        }
        Horario horario = planejar(salaId, filmeId, inicio, id);
        return sessoes.atualizar(id, salaId, filmeId, horario.inicio(), horario.fim())
                .orElseThrow(() -> naoEncontrada(id));
    }

    public void remover(long id) {
        if (sessoes.porId(id).isEmpty()) {
            throw naoEncontrada(id);
        }
        if (sessoes.possuiOcupacoes(id)) {
            throw new ErroDeDominio.Conflito(
                    "sessão " + id + " possui assentos ocupados e não pode ser removida");
        }
        if (!sessoes.remover(id)) {
            throw naoEncontrada(id);
        }
    }

    public List<AssentoDisponibilidade> assentosDaSessao(long id) {
        Sessao sessao = porId(id);
        Set<Long> ocupados = assentos.ocupadosNaSessao(sessao.id());
        return assentos.porSala(sessao.salaId()).stream()
                .map(assento -> new AssentoDisponibilidade(
                        assento.id(), assento.fileira(), assento.numero(),
                        !ocupados.contains(assento.id())))
                .toList();
    }

    /**
     * Ocupa um assento na sessão.
     *
     * <p>A unicidade de (sessão, assento) fica no banco: de duas ocupações simultâneas do
     * mesmo assento, só uma sobrevive, e a outra chega ao cliente como 409.
     */
    public void ocuparAssento(long sessaoId, long assentoId) {
        Sessao sessao = porId(sessaoId);
        Assento assento = assentos.porId(assentoId)
                .filter(candidato -> candidato.salaId().equals(sessao.salaId()))
                .orElseThrow(() -> new ErroDeDominio.NaoEncontrado(
                        "assento " + assentoId + " não pertence a esta sessão"));
        assentos.ocupar(sessao.id(), assento.id());
    }

    /**
     * Confere sala, filme e horário de uma vez e devolve o intervalo calculado.
     *
     * <p>Carregar sala e filme aqui evita rebuscá-los na gravação — e evita o 500 que
     * aparecia quando o registro desaparecia entre a verificação e a busca seguinte.
     * A restrição de exclusão da migração V6 continua sendo a garantia final contra
     * duas gravações simultâneas no mesmo horário.
     */
    private Horario planejar(long salaId, long filmeId, OffsetDateTime inicio, Long sessaoAtual) {
        if (salas.porId(salaId).isEmpty()) {
            throw new ErroDeDominio.NaoEncontrado("sala " + salaId + " não encontrada");
        }
        Filme filme = filmes.porId(filmeId)
                .orElseThrow(() -> new ErroDeDominio.NaoEncontrado("filme " + filmeId + " não encontrado"));
        if (!filme.temDuracaoConhecida()) {
            throw new ErroDeDominio.RegraViolada("filme " + filmeId + " não tem duração cadastrada");
        }

        OffsetDateTime fim = inicio.plusMinutes(filme.duracaoMinutos());
        if (sessoes.existeConflito(salaId, inicio, fim, sessaoAtual)) {
            throw new ErroDeDominio.Conflito(
                    "a sala " + salaId + " já possui uma sessão nesse horário");
        }
        return new Horario(inicio, fim);
    }

    private static ErroDeDominio.NaoEncontrado naoEncontrada(long id) {
        return new ErroDeDominio.NaoEncontrado("sessão " + id + " não encontrada");
    }

    /** O intervalo já validado de uma sessão. */
    private record Horario(OffsetDateTime inicio, OffsetDateTime fim) {
    }
}
