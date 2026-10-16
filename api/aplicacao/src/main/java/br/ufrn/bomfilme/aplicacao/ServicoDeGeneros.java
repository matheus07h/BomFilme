package br.ufrn.bomfilme.aplicacao;

import br.ufrn.bomfilme.dominio.Genero;
import br.ufrn.bomfilme.dominio.RepositorioDeGeneros;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/** Caso de uso dos gêneros, que servem para filtrar o catálogo. */
@ApplicationScoped
public class ServicoDeGeneros {

    @Inject
    RepositorioDeGeneros generos;

    public List<Genero> listar() {
        return generos.listarTodos();
    }
}
