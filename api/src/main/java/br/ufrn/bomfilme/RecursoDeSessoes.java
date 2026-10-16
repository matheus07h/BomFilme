package br.ufrn.bomfilme;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Path("/sessoes")
@Produces(MediaType.APPLICATION_JSON)
public class RecursoDeSessoes {


    @Inject
    RepositorioDeSessoes sessoes;

    @Inject
    RepositorioDeAssentos assentos;

    @Inject
    RepositorioDeSalas salas;

    @Inject
    RepositorioDeFilmes filmes;

    @GET
    public List<Sessao> listar(
            @QueryParam("cinemaId")
            @Positive(message = "cinemaId deve ser positivo") Long cinemaId,
            @QueryParam("salaId")
            @Positive(message = "salaId deve ser positivo") Long salaId,
            @QueryParam("filmeId")
            @Positive(message = "filmeId deve ser positivo") Long filmeId,
            @QueryParam("pagina") @DefaultValue("0")
            @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int pagina,
            @QueryParam("tamanho") @DefaultValue("20")
            @Min(value = 1, message = "tamanho deve ser maior ou igual a um")
            @Max(value = Paginacao.TAMANHO_MAXIMO, message = "tamanho deve ser menor ou igual a 100") int tamanho) {
        return sessoes.listar(new FiltroDeSessoes(cinemaId, salaId, filmeId), pagina, tamanho);
    }

    @GET
    @Path("/{id}")
    public Response porId(@PathParam("id") long id) {
        return sessoes.porId(id)
                .map(sessao -> Response.ok(sessao).build())
                .orElseGet(() -> naoEncontrada("sessão " + id + " não encontrada"));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response criar(@Valid @NotNull(message = "o corpo da requisição é obrigatório") NovaSessao nova) {
        return validarProgramacao(nova, null).orElseGet(() -> {
            Sala sala = salas.porId(nova.salaId()).orElseThrow();
            OffsetDateTime fim = fimDaSessao(nova);
            Sessao sessao = sessoes.criar(sala, nova.filmeId(), nova.inicio(), fim);
            return Response.created(URI.create("/sessoes/" + sessao.id)).entity(sessao).build();
        });
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response atualizar(
            @PathParam("id") long id,
            @Valid @NotNull(message = "o corpo da requisição é obrigatório") NovaSessao dados) {
        if (sessoes.porId(id).isEmpty()) {
            return naoEncontrada("sessão " + id + " não encontrada");
        }
        return validarProgramacao(dados, id).orElseGet(() -> {
            Sala sala = salas.porId(dados.salaId()).orElseThrow();
            return sessoes.atualizar(id, sala, dados.filmeId(), dados.inicio(), fimDaSessao(dados))
                    .map(sessao -> Response.ok(sessao).build())
                    .orElseGet(() -> naoEncontrada("sessão " + id + " não encontrada"));
        });
    }

    @DELETE
    @Path("/{id}")
    public Response remover(@PathParam("id") long id) {
        if (sessoes.porId(id).isEmpty()) {
            return naoEncontrada("sessão " + id + " não encontrada");
        }
        if (sessoes.possuiOcupacoes(id)) {
            return conflito("sessão " + id + " possui assentos ocupados e não pode ser removida");
        }
        if (!sessoes.remover(id)) {
            return naoEncontrada("sessão " + id + " não encontrada");
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/assentos")
    public Response assentosDaSessao(@PathParam("id") long id) {
        return sessoes.porId(id)
                .map(this::listarDisponibilidade)
                .orElseGet(() -> naoEncontrada("sessão " + id + " não encontrada"));
    }

    @POST
    @Path("/{id}/assentos/{assentoId}")
    public Response selecionarAssento(@PathParam("id") long id, @PathParam("assentoId") long assentoId) {
        Optional<Sessao> sessao = sessoes.porId(id);
        if (sessao.isEmpty()) {
            return naoEncontrada("sessão " + id + " não encontrada");
        }

        Optional<Assento> assento = assentos.porId(assentoId);
        if (assento.isEmpty() || !assento.get().sala.id.equals(sessao.get().sala.id)) {
            return naoEncontrada("assento " + assentoId + " não pertence a esta sessão");
        }

        assentos.ocupar(sessao.get(), assento.get());
        return Response.status(Response.Status.CREATED).build();
    }

    /**
     * Confere a sala, o filme e o horário da sessão. Devolve a resposta de erro, se houver;
     * vazio quando a sessão pode ser gravada. A restrição de exclusão da migração V6 continua
     * como garantia final contra duas gravações simultâneas no mesmo horário.
     */
    private Optional<Response> validarProgramacao(NovaSessao dados, Long sessaoAtual) {
        if (salas.porId(dados.salaId()).isEmpty()) {
            return Optional.of(naoEncontrada("sala " + dados.salaId() + " não encontrada"));
        }
        Optional<Filme> filme = filmes.buscarPorId(dados.filmeId());
        if (filme.isEmpty()) {
            return Optional.of(naoEncontrada("filme " + dados.filmeId() + " não encontrado"));
        }
        Integer duracao = filme.get().duracaoMinutos;
        if (duracao == null || duracao <= 0) {
            return Optional.of(Problemas.naoProcessavel(
                    "filme " + dados.filmeId() + " não tem duração cadastrada"));
        }
        OffsetDateTime fim = dados.inicio().plusMinutes(duracao);
        if (sessoes.existeConflito(dados.salaId(), dados.inicio(), fim, sessaoAtual)) {
            return Optional.of(conflito("a sala " + dados.salaId() + " já possui uma sessão nesse horário"));
        }
        return Optional.empty();
    }

    private OffsetDateTime fimDaSessao(NovaSessao dados) {
        Filme filme = filmes.buscarPorId(dados.filmeId()).orElseThrow();
        return dados.inicio().plusMinutes(filme.duracaoMinutos);
    }

    private Response listarDisponibilidade(Sessao sessao) {
        Set<Long> ocupados = assentos.ocupadosNaSessao(sessao.id);
        List<AssentoDisponibilidade> disponibilidade = assentos.porSala(sessao.sala.id).stream()
                .map(assento -> new AssentoDisponibilidade(
                        assento.id, assento.fileira, assento.numero, !ocupados.contains(assento.id)))
                .toList();
        return Response.ok(disponibilidade).build();
    }

    private static Response conflito(String mensagem) {
        return Problemas.conflito(mensagem);
    }

    private static Response naoEncontrada(String mensagem) {
        return Problemas.naoEncontrado(mensagem);
    }
}
