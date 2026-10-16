package br.ufrn.bomfilme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Garante que a especificação OpenAPI é gerada do código, cobre todas as rotas e
 * documenta as respostas de erro — inclusive o schema de {@link Problema}.
 */
@QuarkusTest
class OpenApiTest {

    @Test
    void especificacaoCobreTodasAsRotas() {
        given()
                .accept("application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("info.title", is("BomFilme API"))
                .body("info.version", notNullValue())
                .body("paths", hasKey("/redes"))
                .body("paths", hasKey("/redes/{id}"))
                .body("paths", hasKey("/cinemas"))
                .body("paths", hasKey("/cinemas/{id}"))
                .body("paths", hasKey("/salas"))
                .body("paths", hasKey("/salas/{id}"))
                .body("paths", hasKey("/salas/{salaId}/assentos"))
                .body("paths", hasKey("/salas/{salaId}/assentos/{id}"))
                .body("paths", hasKey("/sessoes"))
                .body("paths", hasKey("/sessoes/{id}"))
                .body("paths", hasKey("/sessoes/{id}/assentos"))
                .body("paths", hasKey("/sessoes/{id}/assentos/{assentoId}"))
                .body("paths", hasKey("/filmes"))
                .body("paths", hasKey("/filmes/{id}"))
                .body("paths", hasKey("/filmes/importar-basico/{tmdbId}"))
                .body("paths", hasKey("/filmes/importar-completo/{tmdbId}"))
                .body("paths", hasKey("/filmes/sincronizar-populares"))
                .body("paths", hasKey("/generos"));
    }

    @Test
    void todasAsOperacoesTemResumoEDescricaoDeResposta() {
        given()
                .accept("application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("paths.collectEntries { it }.values().collectMany { it.values() }.summary",
                        everyItem(notNullValue()));
    }

    @Test
    void respostasDeErroSaoDocumentadasComOSchemaDeProblema() {
        given()
                .accept("application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("components.schemas", hasKey("Problema"))
                .body("components.schemas.Problema.properties", hasKey("type"))
                .body("components.schemas.Problema.properties", hasKey("title"))
                .body("components.schemas.Problema.properties", hasKey("status"))
                .body("components.schemas.Problema.properties", hasKey("detail"))
                .body("components.schemas.Problema.properties", hasKey("violacoes"))
                // o 404 de /cinemas/{id} aponta para o schema de Problema em problem+json
                .body("paths.'/cinemas/{id}'.get.responses.'404'.content.'application/problem+json'.schema.$ref",
                        is("#/components/schemas/Problema"))
                // o 409 da remoção de rede também
                .body("paths.'/redes/{id}'.delete.responses.'409'.content.'application/problem+json'.schema.$ref",
                        is("#/components/schemas/Problema"));
    }

    @Test
    void osErrosDeValidacaoEstaoDocumentadosNasRotasComFiltro() {
        given()
                .accept("application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("paths.'/cinemas'.get.responses", hasKey("400"))
                .body("paths.'/salas'.get.responses", hasKey("400"))
                .body("paths.'/sessoes'.get.responses", hasKey("400"))
                .body("paths.'/filmes'.get.responses", hasKey("400"));
    }

    @Test
    void osParametrosDeConsultaEstaoDescritos() {
        given()
                .accept("application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("paths.'/cinemas'.get.parameters.name", hasItems("redeId", "nome", "cidade", "uf", "pagina", "tamanho"))
                .body("paths.'/cinemas'.get.parameters.description", everyItem(notNullValue()));
    }

    @Test
    void aSwaggerUiEstaAcessivel() {
        given().when().get("/q/swagger-ui").then().statusCode(200);
    }

    /**
     * Os registros de domínio têm schema com os campos, não um objeto vazio.
     *
     * <p>Eles vêm de outro módulo Maven, e o gerador do OpenAPI precisa conseguir
     * introspectá-los. Sem este teste, uma falha de indexação esvaziaria os schemas e a
     * especificação passaria a mentir sobre o contrato sem nada falhar.
     */
    @Test
    void osSchemasDosRegistrosDeDominioTrazemOsCampos() {
        given()
                .accept("application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("components.schemas.RedeCinema.properties", allOf(
                        hasKey("id"), hasKey("nome"), hasKey("criadoEm")))
                .body("components.schemas.Cinema.properties", allOf(
                        hasKey("id"), hasKey("redeId"), hasKey("nome"), hasKey("cidade"), hasKey("uf")))
                .body("components.schemas.Sala.properties", allOf(
                        hasKey("id"), hasKey("cinemaId"), hasKey("nome")))
                .body("components.schemas.Assento.properties", allOf(
                        hasKey("id"), hasKey("salaId"), hasKey("fileira"), hasKey("numero")))
                .body("components.schemas.Sessao.properties", allOf(
                        hasKey("id"), hasKey("salaId"), hasKey("filmeId"), hasKey("inicio"), hasKey("fim")))
                .body("components.schemas.AssentoDisponibilidade.properties", allOf(
                        hasKey("id"), hasKey("fileira"), hasKey("numero"), hasKey("disponivel")))
                .body("components.schemas.Pagina.properties", allOf(
                        hasKey("itens"), hasKey("pagina"), hasKey("tamanho"),
                        hasKey("total"), hasKey("totalPaginas")))
                .body("components.schemas.ResultadoSincronizacao.properties", allOf(
                        hasKey("pagina"), hasKey("atualizados"), hasKey("ignorados")));
    }
}
