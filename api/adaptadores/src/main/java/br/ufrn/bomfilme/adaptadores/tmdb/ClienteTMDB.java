package br.ufrn.bomfilme.adaptadores.tmdb;

import io.quarkus.rest.client.reactive.ClientQueryParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** Cliente HTTP do TMDB. O token vem de variável de ambiente, nunca do código. */
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
            @QueryParam("append_to_response") String appendToResponse);

    @GET
    @Path("/movie/popular")
    ListarFilmesTMDBResponse listarPopulares(@QueryParam("page") @DefaultValue("1") Integer page);

    // TODO: expor importação por nome quando houver tela para o admin escolher entre os
    //  candidatos retornados (ex.: Aladdin 1992 vs 2019).
    @GET
    @Path("/search/movie")
    ListarFilmesTMDBResponse buscarPorNome(
            @QueryParam("query") String nome,
            @QueryParam("page") @DefaultValue("1") Integer page);
}
