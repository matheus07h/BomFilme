package br.ufrn.bomfilme;

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

@Path("/redes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecursoDeRedes {

    @Inject
    RepositorioDeRedes repositorio;

    @Inject
    RepositorioDeCinemas cinemas;

    @GET
    public List<RedeCinema> listar(
            @QueryParam("nome")
            @Size(max = 120, message = "nome deve ter no máximo 120 caracteres") String nome,
            @QueryParam("pagina") @DefaultValue("0")
            @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int pagina,
            @QueryParam("tamanho") @DefaultValue("20")
            @Min(value = 1, message = "tamanho deve ser maior ou igual a um")
            @Max(value = Paginacao.TAMANHO_MAXIMO, message = "tamanho deve ser menor ou igual a 100") int tamanho) {
        return repositorio.listar(nome, pagina, tamanho);
    }

    @GET
    @Path("/{id}")
    public Response porId(@PathParam("id") long id) {
        return repositorio.porId(id)
                .map(rede -> Response.ok(rede).build())
                .orElseGet(() -> naoEncontrada(id));
    }

    @POST
    public Response criar(@Valid @NotNull(message = "o corpo da requisição é obrigatório") NovaRede nova) {
        RedeCinema rede = repositorio.criar(nova.nome().trim());
        return Response.created(URI.create("/redes/" + rede.id)).entity(rede).build();
    }

    @PUT
    @Path("/{id}")
    public Response atualizar(
            @PathParam("id") long id,
            @Valid @NotNull(message = "o corpo da requisição é obrigatório") NovaRede dados) {
        return repositorio.atualizar(id, dados.nome().trim())
                .map(rede -> Response.ok(rede).build())
                .orElseGet(() -> naoEncontrada(id));
    }

    @DELETE
    @Path("/{id}")
    public Response remover(@PathParam("id") long id) {
        if (repositorio.porId(id).isEmpty()) {
            return naoEncontrada(id);
        }
        if (cinemas.existePorRede(id)) {
            return Problemas.conflito("rede " + id + " possui cinemas vinculados e não pode ser removida");
        }
        if (!repositorio.remover(id)) {
            return naoEncontrada(id);
        }
        return Response.noContent().build();
    }

    private static Response naoEncontrada(long id) {
        return Problemas.naoEncontrado("rede " + id + " não encontrada");
    }
}
