package br.ufrn.bomfilme.adaptadores.http;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Identificação da API no OpenAPI. A especificação é gerada do código pelo
 * quarkus-smallrye-openapi e fica em {@code /q/openapi}; a Swagger UI, em
 * {@code /q/swagger-ui}.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "BomFilme API",
                version = "0.1.0",
                description = """
                        API do BomFilme: catálogo de filmes importado do TMDB, redes e cinemas, \
                        salas e assentos, e a programação de sessões.

                        Os erros seguem o formato problem details da RFC 9457, servidos como \
                        `application/problem+json`. O membro `type` identifica a classe do erro e \
                        é estável; `detail` descreve a ocorrência e pode mudar de texto.""",
                contact = @Contact(name = "Equipe BomFilme", url = "https://github.com/matheus07h/BomFilme"),
                license = @License(name = "Uso acadêmico — DIM0547/UFRN")),
        tags = {
                @Tag(name = "Redes", description = "Redes de cinema"),
                @Tag(name = "Cinemas", description = "Cinemas de cada rede"),
                @Tag(name = "Salas", description = "Salas de um cinema e seus assentos"),
                @Tag(name = "Sessões", description = "Programação de sessões e ocupação de assentos"),
                @Tag(name = "Filmes", description = "Catálogo de filmes, importado do TMDB"),
                @Tag(name = "Gêneros", description = "Gêneros usados para filtrar o catálogo")
        })
public class ConfiguracaoOpenApi extends Application {
}
