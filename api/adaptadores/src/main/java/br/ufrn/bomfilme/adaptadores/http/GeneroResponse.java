package br.ufrn.bomfilme.adaptadores.http;

import br.ufrn.bomfilme.dominio.Genero;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "Genero", description = "Gênero usado para filtrar o catálogo")
public record GeneroResponse(Long id, String nome) {

    public static GeneroResponse de(Genero genero) {
        return new GeneroResponse(genero.id(), genero.nome());
    }
}
