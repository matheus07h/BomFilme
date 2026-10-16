package br.ufrn.bomfilme;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Salas de um cinema e, aninhados em cada sala, os seus assentos. */
@Path("/salas")
@Produces({MediaType.APPLICATION_JSON, Problema.MEDIA_TYPE})
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Salas")
public class RecursoDeSalas {

    @Inject
    RepositorioDeSalas salas;

    @Inject
    RepositorioDeCinemas cinemas;

    @Inject
    RepositorioDeAssentos assentos;

    @GET
    @Operation(summary = "Lista as salas", description = "Listagem paginada, com filtro por cinema e por nome.")
    @APIResponse(responseCode = "200", description = "Página de salas, vazia quando não há resultados",
            content = @Content(schema = @Schema(implementation = Sala[].class)))
    @APIResponse(responseCode = "400", description = "Parâmetros de paginação ou filtro inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public List<Sala> listar(
            @Parameter(description = "Filtra pelas salas de um cinema")
            @QueryParam("cinemaId")
            @Positive(message = "cinemaId deve ser positivo") Long cinemaId,
            @Parameter(description = "Filtra pelo nome da sala, sem diferenciar maiúsculas")
            @QueryParam("nome")
            @Size(max = 60, message = "nome deve ter no máximo 60 caracteres") String nome,
            @Parameter(description = "Índice da página, começando em zero")
            @QueryParam("pagina") @DefaultValue("0")
            @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int pagina,
            @Parameter(description = "Quantidade de salas por página, entre 1 e 100")
            @QueryParam("tamanho") @DefaultValue("20")
            @Min(value = 1, message = "tamanho deve ser maior ou igual a um")
            @Max(value = Paginacao.TAMANHO_MAXIMO, message = "tamanho deve ser menor ou igual a 100") int tamanho) {
        return salas.listar(new FiltroDeSalas(cinemaId, nome), pagina, tamanho);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Busca uma sala por id")
    @APIResponse(responseCode = "200", description = "A sala encontrada",
            content = @Content(schema = @Schema(implementation = Sala.class)))
    @APIResponse(responseCode = "404", description = "Não existe sala com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response porId(@Parameter(description = "Id da sala") @PathParam("id") long id) {
        return salas.porId(id)
                .map(sala -> Response.ok(sala).build())
                .orElseGet(() -> salaNaoEncontrada(id));
    }

    @POST
    @Operation(summary = "Cria uma sala em um cinema")
    @APIResponse(responseCode = "201", description = "Sala criada; o endereço vem no cabeçalho Location",
            content = @Content(schema = @Schema(implementation = Sala.class)))
    @APIResponse(responseCode = "400", description = "Corpo ausente ou campos inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Não existe cinema com o id informado",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "O cinema já possui uma sala com esse nome",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response criar(@Valid @NotNull(message = "o corpo da requisição é obrigatório") NovaSala nova) {
        Cinema cinema = cinemas.porId(nova.cinemaId()).orElse(null);
        if (cinema == null) {
            return cinemaNaoEncontrado(nova.cinemaId());
        }
        Sala sala = salas.criar(cinema, nova.nome().trim());
        return Response.created(URI.create("/salas/" + sala.id)).entity(sala).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualiza uma sala")
    @APIResponse(responseCode = "200", description = "Sala atualizada",
            content = @Content(schema = @Schema(implementation = Sala.class)))
    @APIResponse(responseCode = "400", description = "Corpo ausente ou campos inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Não existe sala, ou cinema, com o id informado",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "O cinema já possui outra sala com esse nome",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response atualizar(
            @Parameter(description = "Id da sala") @PathParam("id") long id,
            @Valid @NotNull(message = "o corpo da requisição é obrigatório") NovaSala dados) {
        if (salas.porId(id).isEmpty()) {
            return salaNaoEncontrada(id);
        }
        Cinema cinema = cinemas.porId(dados.cinemaId()).orElse(null);
        if (cinema == null) {
            return cinemaNaoEncontrado(dados.cinemaId());
        }
        return salas.atualizar(id, cinema, dados.nome().trim())
                .map(sala -> Response.ok(sala).build())
                .orElseGet(() -> salaNaoEncontrada(id));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Remove uma sala",
            description = "A remoção é recusada enquanto houver sessões ou assentos vinculados à sala.")
    @APIResponse(responseCode = "204", description = "Sala removida")
    @APIResponse(responseCode = "404", description = "Não existe sala com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "A sala ainda possui sessões ou assentos vinculados",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response remover(@Parameter(description = "Id da sala") @PathParam("id") long id) {
        if (salas.porId(id).isEmpty()) {
            return salaNaoEncontrada(id);
        }
        if (salas.possuiSessoes(id)) {
            return conflito("sala " + id + " possui sessões vinculadas e não pode ser removida");
        }
        if (salas.possuiAssentos(id)) {
            return conflito("sala " + id + " possui assentos vinculados e não pode ser removida");
        }
        if (!salas.remover(id)) {
            return salaNaoEncontrada(id);
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/{salaId}/assentos")
    @Operation(summary = "Lista os assentos de uma sala",
            description = "Ordenados por fileira e número. A fileira do filtro não diferencia maiúsculas.")
    @APIResponse(responseCode = "200", description = "Página de assentos, vazia quando não há resultados",
            content = @Content(schema = @Schema(implementation = Assento[].class)))
    @APIResponse(responseCode = "400", description = "Parâmetros de paginação ou filtro inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Não existe sala com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response listarAssentos(
            @Parameter(description = "Id da sala") @PathParam("salaId") long salaId,
            @Parameter(description = "Filtra por fileira, com uma ou duas letras")
            @QueryParam("fileira")
            @Pattern(regexp = "[A-Za-z]{1,2}", message = "fileira deve ter uma ou duas letras") String fileira,
            @Parameter(description = "Índice da página, começando em zero")
            @QueryParam("pagina") @DefaultValue("0")
            @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int pagina,
            @Parameter(description = "Quantidade de assentos por página, entre 1 e 100")
            @QueryParam("tamanho") @DefaultValue("20")
            @Min(value = 1, message = "tamanho deve ser maior ou igual a um")
            @Max(value = Paginacao.TAMANHO_MAXIMO, message = "tamanho deve ser menor ou igual a 100") int tamanho) {
        if (salas.porId(salaId).isEmpty()) {
            return salaNaoEncontrada(salaId);
        }
        String fileiraNormalizada = fileira == null ? null : fileira.toUpperCase(Locale.ROOT);
        return Response.ok(assentos.listarDaSala(salaId, fileiraNormalizada, pagina, tamanho)).build();
    }

    @GET
    @Path("/{salaId}/assentos/{id}")
    @Operation(summary = "Busca um assento de uma sala")
    @APIResponse(responseCode = "200", description = "O assento encontrado",
            content = @Content(schema = @Schema(implementation = Assento.class)))
    @APIResponse(responseCode = "404", description = "O assento não existe ou não pertence a essa sala",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response assentoPorId(
            @Parameter(description = "Id da sala") @PathParam("salaId") long salaId,
            @Parameter(description = "Id do assento") @PathParam("id") long id) {
        return assentoDaSala(salaId, id)
                .map(assento -> Response.ok(assento).build())
                .orElseGet(() -> assentoNaoEncontrado(salaId, id));
    }

    @POST
    @Path("/{salaId}/assentos")
    @Operation(summary = "Cria um assento em uma sala", description = "A fileira é gravada em maiúsculas.")
    @APIResponse(responseCode = "201", description = "Assento criado; o endereço vem no cabeçalho Location",
            content = @Content(schema = @Schema(implementation = Assento.class)))
    @APIResponse(responseCode = "400", description = "Corpo ausente ou campos inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Não existe sala com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "A sala já possui um assento nessa fileira e número",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response criarAssento(
            @Parameter(description = "Id da sala") @PathParam("salaId") long salaId,
            @Valid @NotNull(message = "o corpo da requisição é obrigatório") NovoAssento novo) {
        Sala sala = salas.porId(salaId).orElse(null);
        if (sala == null) {
            return salaNaoEncontrada(salaId);
        }
        Assento assento = assentos.criar(sala, novo.fileira().toUpperCase(Locale.ROOT), novo.numero());
        return Response.created(URI.create("/salas/" + salaId + "/assentos/" + assento.id))
                .entity(assento)
                .build();
    }

    @PUT
    @Path("/{salaId}/assentos/{id}")
    @Operation(summary = "Atualiza um assento", description = "O assento continua na mesma sala.")
    @APIResponse(responseCode = "200", description = "Assento atualizado",
            content = @Content(schema = @Schema(implementation = Assento.class)))
    @APIResponse(responseCode = "400", description = "Corpo ausente ou campos inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "O assento não existe ou não pertence a essa sala",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "A sala já possui outro assento nessa fileira e número",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response atualizarAssento(
            @Parameter(description = "Id da sala") @PathParam("salaId") long salaId,
            @Parameter(description = "Id do assento") @PathParam("id") long id,
            @Valid @NotNull(message = "o corpo da requisição é obrigatório") NovoAssento dados) {
        if (assentoDaSala(salaId, id).isEmpty()) {
            return assentoNaoEncontrado(salaId, id);
        }
        return assentos.atualizar(id, dados.fileira().toUpperCase(Locale.ROOT), dados.numero())
                .map(assento -> Response.ok(assento).build())
                .orElseGet(() -> assentoNaoEncontrado(salaId, id));
    }

    @DELETE
    @Path("/{salaId}/assentos/{id}")
    @Operation(summary = "Remove um assento",
            description = "A remoção é recusada enquanto o assento estiver ocupado em alguma sessão.")
    @APIResponse(responseCode = "204", description = "Assento removido")
    @APIResponse(responseCode = "404", description = "O assento não existe ou não pertence a essa sala",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "O assento está ocupado em alguma sessão",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response removerAssento(
            @Parameter(description = "Id da sala") @PathParam("salaId") long salaId,
            @Parameter(description = "Id do assento") @PathParam("id") long id) {
        if (assentoDaSala(salaId, id).isEmpty()) {
            return assentoNaoEncontrado(salaId, id);
        }
        if (assentos.possuiOcupacoes(id)) {
            return conflito("assento " + id + " está ocupado em alguma sessão e não pode ser removido");
        }
        if (!assentos.remover(id)) {
            return assentoNaoEncontrado(salaId, id);
        }
        return Response.noContent().build();
    }

    private Optional<Assento> assentoDaSala(long salaId, long id) {
        return assentos.porId(id).filter(assento -> assento.sala.id == salaId);
    }

    private static Response conflito(String mensagem) {
        return Problemas.conflito(mensagem);
    }

    private static Response salaNaoEncontrada(long id) {
        return Problemas.naoEncontrado("sala " + id + " não encontrada");
    }

    private static Response cinemaNaoEncontrado(long id) {
        return Problemas.naoEncontrado("cinema " + id + " não encontrado");
    }

    private static Response assentoNaoEncontrado(long salaId, long id) {
        return Problemas.naoEncontrado("assento " + id + " não encontrado na sala " + salaId);
    }
}
