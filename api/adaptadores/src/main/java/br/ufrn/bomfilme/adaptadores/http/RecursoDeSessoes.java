package br.ufrn.bomfilme.adaptadores.http;

import br.ufrn.bomfilme.aplicacao.ServicoDeSessoes;
import br.ufrn.bomfilme.dominio.AssentoDisponibilidade;
import br.ufrn.bomfilme.dominio.FiltroDeSessoes;
import br.ufrn.bomfilme.dominio.Paginacao;
import br.ufrn.bomfilme.dominio.Sessao;
import br.ufrn.bomfilme.dominio.Pagina;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

@Path("/sessoes")
@Produces({MediaType.APPLICATION_JSON, Problema.MEDIA_TYPE})
@Tag(name = "Sessões")
public class RecursoDeSessoes {

    @Inject
    ServicoDeSessoes servico;

    @GET
    @Operation(summary = "Lista as sessões",
            description = "Listagem paginada, ordenada por horário de início, com filtro por cinema, sala e filme.")
    @APIResponse(responseCode = "200", description = "Página de sessões, vazia quando não há resultados",
            content = @Content(schema = @Schema(implementation = Pagina.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros de paginação ou filtro inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Pagina<Sessao> listar(
            @Parameter(description = "Filtra pelas sessões de um cinema")
            @QueryParam("cinemaId")
            @Positive(message = "cinemaId deve ser positivo") Long cinemaId,
            @Parameter(description = "Filtra pelas sessões de uma sala")
            @QueryParam("salaId")
            @Positive(message = "salaId deve ser positivo") Long salaId,
            @Parameter(description = "Filtra pelas sessões de um filme")
            @QueryParam("filmeId")
            @Positive(message = "filmeId deve ser positivo") Long filmeId,
            @Parameter(description = "Índice da página, começando em zero")
            @QueryParam("pagina") @DefaultValue("0")
            @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int pagina,
            @Parameter(description = "Quantidade de sessões por página, entre 1 e 100")
            @QueryParam("tamanho") @DefaultValue("20")
            @Min(value = 1, message = "tamanho deve ser maior ou igual a um")
            @Max(value = Paginacao.TAMANHO_MAXIMO, message = "tamanho deve ser menor ou igual a 100") int tamanho) {
        return servico.listar(new FiltroDeSessoes(cinemaId, salaId, filmeId), pagina, tamanho);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Busca uma sessão por id")
    @APIResponse(responseCode = "200", description = "A sessão encontrada",
            content = @Content(schema = @Schema(implementation = Sessao.class)))
    @APIResponse(responseCode = "404", description = "Não existe sessão com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Sessao porId(@Parameter(description = "Id da sessão") @PathParam("id") long id) {
        return servico.porId(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Programa uma sessão",
            description = """
                    O horário de fim é calculado a partir da duração do filme, por isso o filme \
                    precisa ter duração cadastrada. Duas sessões da mesma sala não podem se \
                    sobrepor; a garantia final é uma restrição de exclusão no banco, que vale \
                    também para requisições simultâneas.""")
    @APIResponse(responseCode = "201", description = "Sessão criada; o endereço vem no cabeçalho Location",
            content = @Content(schema = @Schema(implementation = Sessao.class)))
    @APIResponse(responseCode = "400", description = "Corpo ausente ou campos inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Não existe sala, ou filme, com o id informado",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "A sala já possui uma sessão nesse horário",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "422", description = "O filme não tem duração cadastrada",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response criar(@Valid @NotNull(message = "o corpo da requisição é obrigatório") NovaSessao nova) {
        Sessao sessao = servico.criar(nova.salaId(), nova.filmeId(), nova.inicio());
        return Response.created(URI.create("/sessoes/" + sessao.id())).entity(sessao).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Reprograma uma sessão",
            description = "A própria sessão é ignorada na verificação de sobreposição de horários.")
    @APIResponse(responseCode = "200", description = "Sessão atualizada",
            content = @Content(schema = @Schema(implementation = Sessao.class)))
    @APIResponse(responseCode = "400", description = "Corpo ausente ou campos inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Não existe sessão, sala ou filme com o id informado",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "A sala já possui outra sessão nesse horário",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "422", description = "O filme não tem duração cadastrada",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Sessao atualizar(
            @Parameter(description = "Id da sessão") @PathParam("id") long id,
            @Valid @NotNull(message = "o corpo da requisição é obrigatório") NovaSessao dados) {
        return servico.atualizar(id, dados.salaId(), dados.filmeId(), dados.inicio());
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Cancela uma sessão",
            description = "O cancelamento é recusado enquanto houver assentos ocupados na sessão.")
    @APIResponse(responseCode = "204", description = "Sessão removida")
    @APIResponse(responseCode = "404", description = "Não existe sessão com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "A sessão já possui assentos ocupados",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response remover(@Parameter(description = "Id da sessão") @PathParam("id") long id) {
        servico.remover(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/assentos")
    @Operation(summary = "Lista os assentos da sessão com a disponibilidade",
            description = "Todos os assentos da sala, cada um marcado como disponível ou ocupado nesta sessão.")
    @APIResponse(responseCode = "200", description = "Assentos da sala e a disponibilidade de cada um",
            content = @Content(schema = @Schema(implementation = AssentoDisponibilidade[].class)))
    @APIResponse(responseCode = "404", description = "Não existe sessão com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public List<AssentoDisponibilidade> assentosDaSessao(
            @Parameter(description = "Id da sessão") @PathParam("id") long id) {
        return servico.assentosDaSessao(id);
    }

    @POST
    @Path("/{id}/assentos/{assentoId}")
    @Operation(summary = "Ocupa um assento na sessão",
            description = """
                    A unicidade por sessão e assento fica no banco, o que recusa a segunda de \
                    duas ocupações simultâneas do mesmo assento.""")
    @APIResponse(responseCode = "201", description = "Assento ocupado")
    @APIResponse(responseCode = "404", description = "A sessão não existe, ou o assento não pertence à sala da sessão",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "O assento já está ocupado nessa sessão",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response selecionarAssento(
            @Parameter(description = "Id da sessão") @PathParam("id") long id,
            @Parameter(description = "Id do assento") @PathParam("assentoId") long assentoId) {
        servico.ocuparAssento(id, assentoId);
        return Response.status(Response.Status.CREATED).build();
    }
}
