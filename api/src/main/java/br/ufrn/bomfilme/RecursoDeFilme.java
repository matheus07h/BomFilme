package br.ufrn.bomfilme;

import br.ufrn.bomfilme.dtos.response.FilmeResponse;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@Path("/filmes")
public class RecursoDeFilme {
    static final int TAMANHO_PADRAO = 20;
    static final int TAMANHO_MAXIMO = 100;
    @Inject
    ServicoDeFilme servico;

    @Inject
    @RestClient
    ClienteTMDB clienteTMDB;
    @Inject
    RepositorioDeFilmes repositorio;

    // RecursoDeFilme — precisa retornar FilmeResponse, não Filme
    @POST
    @Path("/importar-basico/{tmdbId}")
    public FilmeResponse importarBasico(@PathParam("tmdbId") Long tmdbId) {
        return servico.importarBasico(tmdbId);
    }
    @POST
    @Path("/importar-completo/{tmdbId}")
    public Filme importarCompleto(@PathParam("tmdbId") Long tmdbId) {
        return servico.importarCompleto(tmdbId);
    }

    @GET
    @Path("/testar-conexao")
    @Produces(MediaType.APPLICATION_JSON)
    public String testarConexaoTMDB() {
        // Chama o client do TMDB e retorna o JSON recebido direto para quem chamou seu app
        return clienteTMDB.testarAutenticacao();
    }

    @GET
    public List<FilmeResponse> listar(@QueryParam("pagina") @DefaultValue("0") @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int pagina,
                                      @QueryParam("tamanho") @DefaultValue("20")
            @Min(value = 1, message = "tamanho deve ser maior ou igual a um")
            @Max(value = TAMANHO_MAXIMO, message = "tamanho deve ser menor ou igual a 100") int tamanho) {
        return repositorio.listar(pagina,tamanho).stream().map(FilmeResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    public FilmeResponse buscarPorId(@PathParam("id") long id) {
        return servico.buscarPorId(id);
    }

    @GET
    @Path("/buscar-titulo/{titulo}")
    public List<FilmeResponse> buscarPorTitulo(@PathParam("titulo") String titulo) {

    }
}
