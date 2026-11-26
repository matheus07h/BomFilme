package br.ufrn.bomfilme.adaptadores.http;

import br.ufrn.bomfilme.aplicacao.ServicoDeCompras;
import br.ufrn.bomfilme.dominio.Compra;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** A compra é criada em {@code POST /sessoes/{id}/compras}; aqui ela é consultada. */
@Path("/compras")
@Produces({MediaType.APPLICATION_JSON, Problema.MEDIA_TYPE})
@Tag(name = "Compras")
public class RecursoDeCompras {

    @Inject
    ServicoDeCompras servico;

    @GET
    @Path("/{id}")
    @Operation(summary = "Consulta uma compra e o ingresso dela",
            description = """
                    Serve para acompanhar uma compra que ficou PENDENTE: depois do prazo, a \
                    recuperação a confirma, recusa ou expira.""")
    @APIResponse(responseCode = "200", description = "A compra, com o ingresso quando confirmada",
            content = @Content(schema = @Schema(implementation = CompraResponse.class)))
    @APIResponse(responseCode = "404", description = "Não existe compra com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public CompraResponse porId(@Parameter(description = "Id da compra") @PathParam("id") long id) {
        Compra compra = servico.porId(id);
        return CompraResponse.de(compra, servico.ingressoDaCompra(id));
    }
}
