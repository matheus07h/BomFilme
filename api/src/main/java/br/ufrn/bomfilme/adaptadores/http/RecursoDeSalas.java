package br.ufrn.bomfilme.adaptadores.http;

import br.ufrn.bomfilme.aplicacao.ServicoDeSalas;
import br.ufrn.bomfilme.dominio.Assento;
import br.ufrn.bomfilme.dominio.FiltroDeSalas;
import br.ufrn.bomfilme.dominio.Paginacao;
import br.ufrn.bomfilme.dominio.Sala;
import br.ufrn.bomfilme.dominio.Pagina;
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
    ServicoDeSalas servico;

    @GET
    @Operation(summary = "Lista as salas", description = "Listagem paginada, com filtro por cinema e por nome.")
    @APIResponse(responseCode = "200", description = "Página de salas, vazia quando não há resultados",
            content = @Content(schema = @Schema(implementation = Pagina.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros de paginação ou filtro inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Pagina<Sala> listar(
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
        return servico.listar(new FiltroDeSalas(cinemaId, nome), pagina, tamanho);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Busca uma sala por id")
    @APIResponse(responseCode = "200", description = "A sala encontrada",
            content = @Content(schema = @Schema(implementation = Sala.class)))
    @APIResponse(responseCode = "404", description = "Não existe sala com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Sala porId(@Parameter(description = "Id da sala") @PathParam("id") long id) {
        return servico.porId(id);
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
        Sala sala = servico.criar(nova.cinemaId(), nova.nome());
        return Response.created(URI.create("/salas/" + sala.id())).entity(sala).build();
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
    public Sala atualizar(
            @Parameter(description = "Id da sala") @PathParam("id") long id,
            @Valid @NotNull(message = "o corpo da requisição é obrigatório") NovaSala dados) {
        return servico.atualizar(id, dados.cinemaId(), dados.nome());
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
        servico.remover(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{salaId}/assentos")
    @Operation(summary = "Lista os assentos de uma sala",
            description = "Ordenados por fileira e número. A fileira do filtro não diferencia maiúsculas.")
    @APIResponse(responseCode = "200", description = "Página de assentos, vazia quando não há resultados",
            content = @Content(schema = @Schema(implementation = Pagina.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros de paginação ou filtro inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Não existe sala com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Pagina<Assento> listarAssentos(
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
        return servico.listarAssentos(salaId, fileira, pagina, tamanho);
    }

    @GET
    @Path("/{salaId}/assentos/{id}")
    @Operation(summary = "Busca um assento de uma sala")
    @APIResponse(responseCode = "200", description = "O assento encontrado",
            content = @Content(schema = @Schema(implementation = Assento.class)))
    @APIResponse(responseCode = "404", description = "O assento não existe ou não pertence a essa sala",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Assento assentoPorId(
            @Parameter(description = "Id da sala") @PathParam("salaId") long salaId,
            @Parameter(description = "Id do assento") @PathParam("id") long id) {
        return servico.assentoPorId(salaId, id);
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
        Assento assento = servico.criarAssento(salaId, novo.fileira(), novo.numero());
        return Response.created(URI.create("/salas/" + salaId + "/assentos/" + assento.id()))
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
    public Assento atualizarAssento(
            @Parameter(description = "Id da sala") @PathParam("salaId") long salaId,
            @Parameter(description = "Id do assento") @PathParam("id") long id,
            @Valid @NotNull(message = "o corpo da requisição é obrigatório") NovoAssento dados) {
        return servico.atualizarAssento(salaId, id, dados.fileira(), dados.numero());
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
        servico.removerAssento(salaId, id);
        return Response.noContent().build();
    }
}
