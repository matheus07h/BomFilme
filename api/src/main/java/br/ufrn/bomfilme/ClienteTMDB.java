package br.ufrn.bomfilme;

import br.ufrn.bomfilme.dtos.response.FilmeTMDBResponse;
import br.ufrn.bomfilme.dtos.response.ListarFilmesTMDBResponse;
import io.quarkus.rest.client.reactive.ClientQueryParam;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "tmdb-api")
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ClientQueryParam(name = "language", value = "pt-BR")
@ClientHeaderParam(name = "Authorization", value = "${tmdb.auth.token}")
public interface ClienteTMDB {

    @GET
    @Path("/movie/{id}")
    FilmeTMDBResponse buscarPorId(
            @PathParam("id") Long id,
            @QueryParam("append_to_response") String appendToResponse
    );

    @GET
    @Path("/movie/popular")
    ListarFilmesTMDBResponse listarPopulares(
            @QueryParam("page") @DefaultValue("1") Integer page
    );

    // TODO: expor busca/importação por nome quando houver front-end para o admin escolher entre os candidatos retornados
    //  (ex.: Aladdin 1992 vs 2019). Ver ServicoDeFilme/RecursoDeFilme.
    @GET
    @Path("/search/movie")
    ListarFilmesTMDBResponse buscarPorNome(@QueryParam("query") String nome, @QueryParam("page") @DefaultValue("1") Integer page);

    @GET
    @Path("/authentication")
    String testarAutenticacao();
}