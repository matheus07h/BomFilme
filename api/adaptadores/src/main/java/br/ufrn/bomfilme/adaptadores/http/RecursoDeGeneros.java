package br.ufrn.bomfilme.adaptadores.http;

import br.ufrn.bomfilme.aplicacao.ServicoDeGeneros;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/generos")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Gêneros")
public class RecursoDeGeneros {

    @Inject
    ServicoDeGeneros servico;

    @GET
    @Operation(summary = "Lista os gêneros do catálogo",
            description = """
                    Os gêneros nascem da importação de filmes do TMDB: um gênero só aparece aqui \
                    depois que algum filme daquele gênero entrou no catálogo. Serve para preencher \
                    o filtro `generoId` de `GET /filmes`.""")
    @APIResponse(responseCode = "200",
            description = "Gêneros em ordem alfabética, vazio quando nenhum filme foi importado",
            content = @Content(schema = @Schema(implementation = GeneroResponse[].class)))
    public List<GeneroResponse> listar() {
        return servico.listar().stream().map(GeneroResponse::de).toList();
    }
}
