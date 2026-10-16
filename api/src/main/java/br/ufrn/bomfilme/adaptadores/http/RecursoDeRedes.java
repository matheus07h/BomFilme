package br.ufrn.bomfilme.adaptadores.http;

import br.ufrn.bomfilme.aplicacao.ServicoDeRedes;
import br.ufrn.bomfilme.dominio.Paginacao;
import br.ufrn.bomfilme.dominio.RedeCinema;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

@Path("/redes")
@Produces({MediaType.APPLICATION_JSON, Problema.MEDIA_TYPE})
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Redes")
public class RecursoDeRedes {

    @Inject
    ServicoDeRedes servico;

    @GET
    @Operation(summary = "Lista as redes de cinema",
            description = "Listagem paginada, ordenada por id para que a navegação entre páginas seja estável.")
    @APIResponse(responseCode = "200", description = "Página de redes, vazia quando não há resultados",
            content = @Content(schema = @Schema(implementation = RedeCinema[].class)))
    @APIResponse(responseCode = "400", description = "Parâmetros de paginação ou filtro inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public List<RedeCinema> listar(
            @Parameter(description = "Filtra pelo nome da rede, sem diferenciar maiúsculas")
            @QueryParam("nome")
            @Size(max = 120, message = "nome deve ter no máximo 120 caracteres") String nome,
            @Parameter(description = "Índice da página, começando em zero")
            @QueryParam("pagina") @DefaultValue("0")
            @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int pagina,
            @Parameter(description = "Quantidade de redes por página, entre 1 e 100")
            @QueryParam("tamanho") @DefaultValue("20")
            @Min(value = 1, message = "tamanho deve ser maior ou igual a um")
            @Max(value = Paginacao.TAMANHO_MAXIMO, message = "tamanho deve ser menor ou igual a 100") int tamanho) {
        return servico.listar(nome, pagina, tamanho);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Busca uma rede por id")
    @APIResponse(responseCode = "200", description = "A rede encontrada",
            content = @Content(schema = @Schema(implementation = RedeCinema.class)))
    @APIResponse(responseCode = "404", description = "Não existe rede com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public RedeCinema porId(@Parameter(description = "Id da rede") @PathParam("id") long id) {
        return servico.porId(id);
    }

    @POST
    @Operation(summary = "Cria uma rede")
    @APIResponse(responseCode = "201", description = "Rede criada; o endereço vem no cabeçalho Location",
            content = @Content(schema = @Schema(implementation = RedeCinema.class)))
    @APIResponse(responseCode = "400", description = "Corpo ausente ou campos inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "Já existe uma rede com esse nome",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response criar(@Valid @NotNull(message = "o corpo da requisição é obrigatório") NovaRede nova) {
        RedeCinema rede = servico.criar(nova.nome());
        return Response.created(URI.create("/redes/" + rede.id())).entity(rede).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualiza uma rede")
    @APIResponse(responseCode = "200", description = "Rede atualizada",
            content = @Content(schema = @Schema(implementation = RedeCinema.class)))
    @APIResponse(responseCode = "400", description = "Corpo ausente ou campos inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Não existe rede com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "Já existe outra rede com esse nome",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public RedeCinema atualizar(
            @Parameter(description = "Id da rede") @PathParam("id") long id,
            @Valid @NotNull(message = "o corpo da requisição é obrigatório") NovaRede dados) {
        return servico.atualizar(id, dados.nome());
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Remove uma rede",
            description = "A remoção é recusada enquanto houver cinemas vinculados à rede.")
    @APIResponse(responseCode = "204", description = "Rede removida")
    @APIResponse(responseCode = "404", description = "Não existe rede com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "A rede ainda possui cinemas vinculados",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response remover(@Parameter(description = "Id da rede") @PathParam("id") long id) {
        servico.remover(id);
        return Response.noContent().build();
    }
}
