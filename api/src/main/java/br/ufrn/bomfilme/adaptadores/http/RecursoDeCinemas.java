package br.ufrn.bomfilme.adaptadores.http;

import br.ufrn.bomfilme.aplicacao.ServicoDeCinemas;
import br.ufrn.bomfilme.dominio.Cinema;
import br.ufrn.bomfilme.dominio.FiltroDeCinemas;
import br.ufrn.bomfilme.dominio.Paginacao;
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

@Path("/cinemas")
@Produces({MediaType.APPLICATION_JSON, Problema.MEDIA_TYPE})
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Cinemas")
public class RecursoDeCinemas {

    @Inject
    ServicoDeCinemas servico;

    @GET
    @Operation(summary = "Lista os cinemas",
            description = """
                    Listagem paginada, ordenada por id para que a navegação entre páginas seja \
                    estável. Os filtros de nome e cidade não diferenciam maiúsculas.""")
    @APIResponse(responseCode = "200", description = "Página de cinemas, vazia quando não há resultados",
            content = @Content(schema = @Schema(implementation = Cinema[].class)))
    @APIResponse(responseCode = "400", description = "Parâmetros de paginação ou filtro inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public List<Cinema> listar(
            @Parameter(description = "Filtra pelos cinemas de uma rede")
            @QueryParam("redeId")
            @Positive(message = "redeId deve ser positivo") Long redeId,
            @Parameter(description = "Filtra pelo nome do cinema")
            @QueryParam("nome")
            @Size(max = 120, message = "nome deve ter no máximo 120 caracteres") String nome,
            @Parameter(description = "Filtra pela cidade do cinema")
            @QueryParam("cidade")
            @Size(max = 120, message = "cidade deve ter no máximo 120 caracteres") String cidade,
            @Parameter(description = "Filtra pela UF, com exatamente duas letras")
            @QueryParam("uf")
            @Pattern(regexp = "[A-Za-z]{2}", message = "uf deve ter exatamente duas letras") String uf,
            @Parameter(description = "Índice da página, começando em zero")
            @QueryParam("pagina") @DefaultValue("0")
            @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int pagina,
            @Parameter(description = "Quantidade de cinemas por página, entre 1 e 100")
            @QueryParam("tamanho") @DefaultValue("20")
            @Min(value = 1, message = "tamanho deve ser maior ou igual a um")
            @Max(value = Paginacao.TAMANHO_MAXIMO, message = "tamanho deve ser menor ou igual a 100") int tamanho) {
        return servico.listar(new FiltroDeCinemas(redeId, nome, cidade, uf), pagina, tamanho);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Busca um cinema por id")
    @APIResponse(responseCode = "200", description = "O cinema encontrado",
            content = @Content(schema = @Schema(implementation = Cinema.class)))
    @APIResponse(responseCode = "404", description = "Não existe cinema com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Cinema porId(@Parameter(description = "Id do cinema") @PathParam("id") long id) {
        return servico.porId(id);
    }

    @POST
    @Operation(summary = "Cria um cinema em uma rede", description = "A UF é gravada em maiúsculas.")
    @APIResponse(responseCode = "201", description = "Cinema criado; o endereço vem no cabeçalho Location",
            content = @Content(schema = @Schema(implementation = Cinema.class)))
    @APIResponse(responseCode = "400", description = "Corpo ausente ou campos inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Não existe rede com o id informado",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "A rede já possui um cinema com esse nome",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response criar(@Valid @NotNull(message = "o corpo da requisição é obrigatório") NovoCinema novo) {
        Cinema cinema = servico.criar(novo.redeId(), novo.nome(), novo.cidade(), novo.uf());
        return Response.created(URI.create("/cinemas/" + cinema.id())).entity(cinema).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualiza um cinema")
    @APIResponse(responseCode = "200", description = "Cinema atualizado",
            content = @Content(schema = @Schema(implementation = Cinema.class)))
    @APIResponse(responseCode = "400", description = "Corpo ausente ou campos inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Não existe cinema, ou rede, com o id informado",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "A rede já possui outro cinema com esse nome",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Cinema atualizar(
            @Parameter(description = "Id do cinema") @PathParam("id") long id,
            @Valid @NotNull(message = "o corpo da requisição é obrigatório") NovoCinema dados) {
        return servico.atualizar(id, dados.redeId(), dados.nome(), dados.cidade(), dados.uf());
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Remove um cinema",
            description = "A remoção é recusada enquanto houver salas vinculadas ao cinema.")
    @APIResponse(responseCode = "204", description = "Cinema removido")
    @APIResponse(responseCode = "404", description = "Não existe cinema com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "O cinema ainda possui salas vinculadas",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response remover(@Parameter(description = "Id do cinema") @PathParam("id") long id) {
        servico.remover(id);
        return Response.noContent().build();
    }
}
