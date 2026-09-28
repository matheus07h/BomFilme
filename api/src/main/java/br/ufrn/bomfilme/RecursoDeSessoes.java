package br.ufrn.bomfilme;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Path("/sessoes")
@Produces(MediaType.APPLICATION_JSON)
public class RecursoDeSessoes {

    @Inject
    RepositorioDeSessoes sessoes;

    @Inject
    RepositorioDeAssentos assentos;

    @GET
    @Path("/{id}/assentos")
    public Response assentosDaSessao(@PathParam("id") long id) {
        return sessoes.porId(id)
                .map(this::listarDisponibilidade)
                .orElseGet(() -> naoEncontrada("sessão " + id + " não encontrada"));
    }

    @POST
    @Path("/{id}/assentos/{assentoId}")
    public Response selecionarAssento(@PathParam("id") long id, @PathParam("assentoId") long assentoId) {
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

    private Response listarDisponibilidade(Sessao sessao) {
        Set<Long> ocupados = assentos.ocupadosNaSessao(sessao.id);
        List<AssentoDisponibilidade> disponibilidade = assentos.porSala(sessao.sala.id).stream()
                .map(assento -> new AssentoDisponibilidade(
                        assento.id, assento.fileira, assento.numero, !ocupados.contains(assento.id)))
                .toList();
        return Response.ok(disponibilidade).build();
    }

    private static Response naoEncontrada(String mensagem) {
        return Response.status(Response.Status.NOT_FOUND).entity(new Erro(mensagem)).build();
    }
}
