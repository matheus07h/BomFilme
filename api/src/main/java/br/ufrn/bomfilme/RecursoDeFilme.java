package br.ufrn.bomfilme;

import br.ufrn.bomfilme.dtos.response.FilmeResponse;
import br.ufrn.bomfilme.dtos.response.FilmeResumoResponse;
import br.ufrn.bomfilme.utils.FiltroDeFilmes;
import br.ufrn.bomfilme.utils.Pagina;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@Path("/filmes")
public class RecursoDeFilme {
    static final int TAMANHO_MAXIMO = 100;
    @Inject
    ServicoDeFilme servico;

    @Inject
    @RestClient
    ClienteTMDB clienteTMDB;
    @Inject
    RepositorioDeFilmes repositorio;

    @POST
    @Path("/importar-basico/{tmdbId}")
    public FilmeResponse importarBasico(@PathParam("tmdbId") Long tmdbId) {
        return servico.importarBasico(tmdbId);
    }
    @POST
    @Path("/importar-completo/{tmdbId}")
    public FilmeResponse importarCompleto(@PathParam("tmdbId") Long tmdbId) {
        return servico.importarCompleto(tmdbId);
    }

    @GET
    @Path("/testar-conexao")
    @Produces(MediaType.APPLICATION_JSON)
    public String testarConexaoTMDB() {
        return clienteTMDB.testarAutenticacao();
    }

    @GET
    public Pagina<FilmeResumoResponse> listar(
            @QueryParam("titulo") @Size(max = 100) String titulo,
            @QueryParam("generoId") Long generoId,
            @QueryParam("ano") @Min(1888) @Max(2100) Integer ano,
            @QueryParam("ordenarPor") @DefaultValue("titulo")
            @Pattern(regexp = "titulo|lancamento|nota|popularidade") String ordenarPor,
            @QueryParam("direcao") @DefaultValue("asc") @Pattern(regexp = "asc|desc") String direcao,
            @QueryParam("pagina") @DefaultValue("0") @Min(0) int pagina,
            @QueryParam("tamanho") @DefaultValue("20") @Min(1) @Max(TAMANHO_MAXIMO) int tamanho) {
        return servico.listar(new FiltroDeFilmes(titulo, generoId, ano, ordenarPor, direcao), pagina, tamanho);
    }

    @POST
    @Path("/sincronizar-populares")
    public ResultadoSincronizacao sincronizarPopulares(
            @QueryParam("pagina") @DefaultValue("1") @Min(1) @Max(500) int pagina) {
        return servico.sincronizarPopulares(pagina);
    }

    @GET
    @Path("/{id}")
    public FilmeResponse buscarPorId(@PathParam("id") long id) {
        return servico.buscarPorId(id);
    }

    @DELETE
    @Path("/{id}")
    public void deletar(@PathParam("id") long id) {
        servico.deletar(id);
    }
}
