package br.ufrn.bomfilme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import br.ufrn.bomfilme.dominio.Paginacao;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Garante que os erros da API saem no formato <em>problem details</em> da RFC 9457,
 * com {@code application/problem+json} e os membros {@code type}, {@code title},
 * {@code status} e {@code detail}.
 */
@QuarkusTest
class ProblemDetailsTest {

    @Test
    void recursoInexistenteRespondeProblemDetails() {
        given()
                .when().get("/cinemas/{id}", 999_999_999L)
                .then()
                .statusCode(404)
                .contentType(containsString("application/problem+json"))
                .body("type", is("urn:bomfilme:erro:nao-encontrado"))
                .body("title", is("Recurso não encontrado"))
                .body("status", is(404))
                .body("detail", notNullValue());
    }

    @Test
    void entradaInvalidaRespondeProblemDetailsComAsViolacoes() {
        given()
                .queryParam("tamanho", Paginacao.TAMANHO_MAXIMO + 1)
                .when().get("/cinemas")
                .then()
                .statusCode(400)
                .contentType(containsString("application/problem+json"))
                .body("type", is("urn:bomfilme:erro:entrada-invalida"))
                .body("title", is("Entrada inválida"))
                .body("status", is(400))
                .body("violacoes", hasItem(startsWith("tamanho:")));
    }

    @Test
    void corpoObrigatorioAusenteRespondeProblemDetails() {
        given()
                .contentType("application/json")
                .when().post("/redes")
                .then()
                .statusCode(400)
                .contentType(containsString("application/problem+json"))
                .body("status", is(400))
                .body("violacoes", notNullValue());
    }

    @Test
    void todoTipoDeProblemaUsaOPrefixoDaAplicacao() {
        given()
                .when().get("/sessoes/{id}", 999_999_999L)
                .then()
                .statusCode(404)
                .body("type", startsWith("urn:bomfilme:erro:"));
    }
}
