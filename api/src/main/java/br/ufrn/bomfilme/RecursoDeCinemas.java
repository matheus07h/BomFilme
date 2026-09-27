package br.ufrn.bomfilme;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Locale;

@Path("/cinemas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecursoDeCinemas {

    static final int TAMANHO_PADRAO = 20;
    static final int TAMANHO_MAXIMO = 100;

    @Inject
    RepositorioDeCinemas cinemas;

    @Inject
    RepositorioDeRedes redes;

    @GET
    public List<Cinema> listar(
            @QueryParam("redeId") Long redeId,
            @QueryParam("pagina") @DefaultValue("0")
            @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int pagina,
            @QueryParam("tamanho") @DefaultValue("20")
            @Min(value = 1, message = "tamanho deve ser maior ou igual a um")
            @Max(value = TAMANHO_MAXIMO, message = "tamanho deve ser menor ou igual a 100") int tamanho) {
        return redeId == null
                ? cinemas.listar(pagina, tamanho)
                : cinemas.porRede(redeId, pagina, tamanho);
    }

    @GET
    @Path("/{id}")
    public Response porId(@PathParam("id") long id) {
        return cinemas.porId(id)
                .map(cinema -> Response.ok(cinema).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new Erro("cinema " + id + " não encontrado"))
                        .build());
    }

    @POST
    public Response criar(@Valid @NotNull(message = "o corpo da requisição é obrigatório") NovoCinema novo) {
        RedeCinema rede = redes.porId(novo.redeId()).orElse(null);
        if (rede == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new Erro("rede " + novo.redeId() + " não encontrada"))
                    .build();
        }
        Cinema cinema = cinemas.criar(
                rede,
                novo.nome().trim(),
                novo.cidade().trim(),
                novo.uf().toUpperCase(Locale.ROOT));
        return Response.created(URI.create("/cinemas/" + cinema.id)).entity(cinema).build();
    }
}
