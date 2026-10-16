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

@Path("/cinemas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecursoDeCinemas {

    @Inject
    RepositorioDeCinemas cinemas;

    @Inject
    RepositorioDeRedes redes;

    @GET
    public List<Cinema> listar(
            @QueryParam("redeId")
            @Positive(message = "redeId deve ser positivo") Long redeId,
            @QueryParam("nome")
            @Size(max = 120, message = "nome deve ter no máximo 120 caracteres") String nome,
            @QueryParam("cidade")
            @Size(max = 120, message = "cidade deve ter no máximo 120 caracteres") String cidade,
            @QueryParam("uf")
            @Pattern(regexp = "[A-Za-z]{2}", message = "uf deve ter exatamente duas letras") String uf,
            @QueryParam("pagina") @DefaultValue("0")
            @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int pagina,
            @QueryParam("tamanho") @DefaultValue("20")
            @Min(value = 1, message = "tamanho deve ser maior ou igual a um")
            @Max(value = Paginacao.TAMANHO_MAXIMO, message = "tamanho deve ser menor ou igual a 100") int tamanho) {
        return cinemas.listar(new FiltroDeCinemas(redeId, nome, cidade, uf), pagina, tamanho);
    }

    @GET
    @Path("/{id}")
    public Response porId(@PathParam("id") long id) {
        return cinemas.porId(id)
                .map(cinema -> Response.ok(cinema).build())
                .orElseGet(() -> cinemaNaoEncontrado(id));
    }

    @POST
    public Response criar(@Valid @NotNull(message = "o corpo da requisição é obrigatório") NovoCinema novo) {
        RedeCinema rede = redes.porId(novo.redeId()).orElse(null);
        if (rede == null) {
            return redeNaoEncontrada(novo.redeId());
        }
        Cinema cinema = cinemas.criar(
                rede,
                novo.nome().trim(),
                novo.cidade().trim(),
                novo.uf().toUpperCase(Locale.ROOT));
        return Response.created(URI.create("/cinemas/" + cinema.id)).entity(cinema).build();
    }

    @PUT
    @Path("/{id}")
    public Response atualizar(
            @PathParam("id") long id,
            @Valid @NotNull(message = "o corpo da requisição é obrigatório") NovoCinema dados) {
        if (cinemas.porId(id).isEmpty()) {
            return cinemaNaoEncontrado(id);
        }
        RedeCinema rede = redes.porId(dados.redeId()).orElse(null);
        if (rede == null) {
            return redeNaoEncontrada(dados.redeId());
        }
        return cinemas.atualizar(
                        id,
                        rede,
                        dados.nome().trim(),
                        dados.cidade().trim(),
                        dados.uf().toUpperCase(Locale.ROOT))
                .map(cinema -> Response.ok(cinema).build())
                .orElseGet(() -> cinemaNaoEncontrado(id));
    }

    @DELETE
    @Path("/{id}")
    public Response remover(@PathParam("id") long id) {
        if (cinemas.porId(id).isEmpty()) {
            return cinemaNaoEncontrado(id);
        }
        if (cinemas.possuiSalas(id)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new Erro("cinema " + id + " possui salas vinculadas e não pode ser removido"))
                    .build();
        }
        if (!cinemas.remover(id)) {
            return cinemaNaoEncontrado(id);
        }
        return Response.noContent().build();
    }

    private static Response cinemaNaoEncontrado(long id) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new Erro("cinema " + id + " não encontrado"))
                .build();
    }

    private static Response redeNaoEncontrada(long id) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new Erro("rede " + id + " não encontrada"))
                .build();
    }
}
