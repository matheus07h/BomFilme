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

/** Salas de um cinema e, aninhados em cada sala, os seus assentos. */
@Path("/salas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecursoDeSalas {

    @Inject
    RepositorioDeSalas salas;

    @Inject
    RepositorioDeCinemas cinemas;

    @Inject
    RepositorioDeAssentos assentos;

    @GET
    public List<Sala> listar(
            @QueryParam("cinemaId")
            @Positive(message = "cinemaId deve ser positivo") Long cinemaId,
            @QueryParam("nome")
            @Size(max = 60, message = "nome deve ter no máximo 60 caracteres") String nome,
            @QueryParam("pagina") @DefaultValue("0")
            @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int pagina,
            @QueryParam("tamanho") @DefaultValue("20")
            @Min(value = 1, message = "tamanho deve ser maior ou igual a um")
            @Max(value = Paginacao.TAMANHO_MAXIMO, message = "tamanho deve ser menor ou igual a 100") int tamanho) {
        return salas.listar(new FiltroDeSalas(cinemaId, nome), pagina, tamanho);
    }

    @GET
    @Path("/{id}")
    public Response porId(@PathParam("id") long id) {
        return salas.porId(id)
                .map(sala -> Response.ok(sala).build())
                .orElseGet(() -> salaNaoEncontrada(id));
    }

    @POST
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
    public Response atualizar(
            @PathParam("id") long id,
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
    public Response remover(@PathParam("id") long id) {
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
    public Response listarAssentos(
            @PathParam("salaId") long salaId,
            @QueryParam("fileira")
            @Pattern(regexp = "[A-Za-z]{1,2}", message = "fileira deve ter uma ou duas letras") String fileira,
            @QueryParam("pagina") @DefaultValue("0")
            @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int pagina,
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
    public Response assentoPorId(@PathParam("salaId") long salaId, @PathParam("id") long id) {
        return assentoDaSala(salaId, id)
                .map(assento -> Response.ok(assento).build())
                .orElseGet(() -> assentoNaoEncontrado(salaId, id));
    }

    @POST
    @Path("/{salaId}/assentos")
    public Response criarAssento(
            @PathParam("salaId") long salaId,
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
    public Response atualizarAssento(
            @PathParam("salaId") long salaId,
            @PathParam("id") long id,
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
    public Response removerAssento(@PathParam("salaId") long salaId, @PathParam("id") long id) {
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
        return Response.status(Response.Status.CONFLICT).entity(new Erro(mensagem)).build();
    }

    private static Response salaNaoEncontrada(long id) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new Erro("sala " + id + " não encontrada"))
                .build();
    }

    private static Response cinemaNaoEncontrado(long id) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new Erro("cinema " + id + " não encontrado"))
                .build();
    }

    private static Response assentoNaoEncontrado(long salaId, long id) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new Erro("assento " + id + " não encontrado na sala " + salaId))
                .build();
    }
}
