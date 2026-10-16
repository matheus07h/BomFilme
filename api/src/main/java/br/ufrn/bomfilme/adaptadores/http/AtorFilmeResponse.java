package br.ufrn.bomfilme.adaptadores.http;

import br.ufrn.bomfilme.dominio.Participacao;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "AtorFilme", description = "Uma pessoa do elenco e o personagem que interpreta")
public record AtorFilmeResponse(String nomeAtor, String personagem) {

    public static AtorFilmeResponse de(Participacao participacao) {
        return new AtorFilmeResponse(participacao.pessoa().nome(), participacao.personagem());
    }
}
