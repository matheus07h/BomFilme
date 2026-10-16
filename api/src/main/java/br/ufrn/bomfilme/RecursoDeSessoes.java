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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/sessoes")
@Produces({MediaType.APPLICATION_JSON, Problema.MEDIA_TYPE})
@Tag(name = "Sessões")
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
    @Operation(summary = "Lista as sessões",
            description = "Listagem paginada, ordenada por horário de início, com filtro por cinema, sala e filme.")
    @APIResponse(responseCode = "200", description = "Página de sessões, vazia quando não há resultados",
            content = @Content(schema = @Schema(implementation = Sessao[].class)))
    @APIResponse(responseCode = "400", description = "Parâmetros de paginação ou filtro inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public List<Sessao> listar(
            @Parameter(description = "Filtra pelas sessões de um cinema")
            @QueryParam("cinemaId")
            @Positive(message = "cinemaId deve ser positivo") Long cinemaId,
            @Parameter(description = "Filtra pelas sessões de uma sala")
            @QueryParam("salaId")
            @Positive(message = "salaId deve ser positivo") Long salaId,
            @Parameter(description = "Filtra pelas sessões de um filme")
            @QueryParam("filmeId")
            @Positive(message = "filmeId deve ser positivo") Long filmeId,
            @Parameter(description = "Índice da página, começando em zero")
            @QueryParam("pagina") @DefaultValue("0")
            @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int pagina,
            @Parameter(description = "Quantidade de sessões por página, entre 1 e 100")
            @QueryParam("tamanho") @DefaultValue("20")
            @Min(value = 1, message = "tamanho deve ser maior ou igual a um")
            @Max(value = Paginacao.TAMANHO_MAXIMO, message = "tamanho deve ser menor ou igual a 100") int tamanho) {
        return sessoes.listar(new FiltroDeSessoes(cinemaId, salaId, filmeId), pagina, tamanho);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Busca uma sessão por id")
    @APIResponse(responseCode = "200", description = "A sessão encontrada",
            content = @Content(schema = @Schema(implementation = Sessao.class)))
    @APIResponse(responseCode = "404", description = "Não existe sessão com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response porId(@Parameter(description = "Id da sessão") @PathParam("id") long id) {
        return sessoes.porId(id)
                .map(sessao -> Response.ok(sessao).build())
                .orElseGet(() -> naoEncontrada("sessão " + id + " não encontrada"));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Programa uma sessão",
            description = """
                    O horário de fim é calculado a partir da duração do filme, por isso o filme \
                    precisa ter duração cadastrada. Duas sessões da mesma sala não podem se \
                    sobrepor; a garantia final é uma restrição de exclusão no banco, que vale \
                    também para requisições simultâneas.""")
    @APIResponse(responseCode = "201", description = "Sessão criada; o endereço vem no cabeçalho Location",
            content = @Content(schema = @Schema(implementation = Sessao.class)))
    @APIResponse(responseCode = "400", description = "Corpo ausente ou campos inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Não existe sala, ou filme, com o id informado",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "A sala já possui uma sessão nesse horário",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "422", description = "O filme não tem duração cadastrada",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
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
    @Operation(summary = "Reprograma uma sessão",
            description = "A própria sessão é ignorada na verificação de sobreposição de horários.")
    @APIResponse(responseCode = "200", description = "Sessão atualizada",
            content = @Content(schema = @Schema(implementation = Sessao.class)))
    @APIResponse(responseCode = "400", description = "Corpo ausente ou campos inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Não existe sessão, sala ou filme com o id informado",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "A sala já possui outra sessão nesse horário",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "422", description = "O filme não tem duração cadastrada",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response atualizar(
            @Parameter(description = "Id da sessão") @PathParam("id") long id,
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
    @Operation(summary = "Cancela uma sessão",
            description = "O cancelamento é recusado enquanto houver assentos ocupados na sessão.")
    @APIResponse(responseCode = "204", description = "Sessão removida")
    @APIResponse(responseCode = "404", description = "Não existe sessão com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "A sessão já possui assentos ocupados",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response remover(@Parameter(description = "Id da sessão") @PathParam("id") long id) {
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
    @Operation(summary = "Lista os assentos da sessão com a disponibilidade",
            description = "Todos os assentos da sala, cada um marcado como disponível ou ocupado nesta sessão.")
    @APIResponse(responseCode = "200", description = "Assentos da sala e a disponibilidade de cada um",
            content = @Content(schema = @Schema(implementation = AssentoDisponibilidade[].class)))
    @APIResponse(responseCode = "404", description = "Não existe sessão com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response assentosDaSessao(@Parameter(description = "Id da sessão") @PathParam("id") long id) {
        return sessoes.porId(id)
                .map(this::listarDisponibilidade)
                .orElseGet(() -> naoEncontrada("sessão " + id + " não encontrada"));
    }

    @POST
    @Path("/{id}/assentos/{assentoId}")
    @Operation(summary = "Ocupa um assento na sessão",
            description = """
                    A unicidade por sessão e assento fica no banco, o que recusa a segunda de \
                    duas ocupações simultâneas do mesmo assento.""")
    @APIResponse(responseCode = "201", description = "Assento ocupado")
    @APIResponse(responseCode = "404", description = "A sessão não existe, ou o assento não pertence à sala da sessão",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "O assento já está ocupado nessa sessão",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response selecionarAssento(
            @Parameter(description = "Id da sessão") @PathParam("id") long id,
            @Parameter(description = "Id do assento") @PathParam("assentoId") long assentoId) {
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
