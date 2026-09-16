package br.ufrn.bomfilme;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
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

    @GET
    public List<RedeCinema> listar() {
        return repositorio.listar();
    }

    @GET
    @Path("/{id}")
    public Response porId(@PathParam("id") long id) {
        return repositorio.porId(id)
                .map(rede -> Response.ok(rede).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new Erro("rede " + id + " não encontrada"))
                        .build());
    }

    @POST
    public Response criar(@Valid @NotNull(message = "o corpo da requisição é obrigatório") NovaRede nova) {
        RedeCinema rede = repositorio.criar(nova.nome().trim());
        return Response.created(URI.create("/redes/" + rede.id)).entity(rede).build();
    }
}
