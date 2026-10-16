package br.ufrn.bomfilme;

import br.ufrn.bomfilme.dtos.response.GeneroResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.util.List;

@Path("/generos")
public class RecursoDeGeneros {
    @Inject
    RepositorioDeGeneros repositorioDeGeneros;
    @GET
    public List<GeneroResponse> listar(){
        return repositorioDeGeneros.listarTodos().stream().map(GeneroResponse::from).toList();
    }
}
