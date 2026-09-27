package br.ufrn.bomfilme;

import br.ufrn.bomfilme.dtos.response.FilmeTMDBResponse;
import br.ufrn.bomfilme.dtos.response.ListarFilmesTMDBResponse;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "tmdb-api")
@Path("/movie")
@ClientHeaderParam(name="authorization", value= "Bearer {tmdb.api.token}")
public interface ClienteTMDB {
    @GET
    @Path("/{id}")
    FilmeTMDBResponse buscarPorId(@PathParam("id") Long id, @QueryParam("append_to_response") String appendToResponse);

    @GET
    @Path("/popular")
    ListarFilmesTMDBResponse listarPopulares();
}
