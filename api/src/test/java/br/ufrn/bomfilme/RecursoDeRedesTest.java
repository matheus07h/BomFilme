package br.ufrn.bomfilme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * Teste de ponta a ponta: sobe a aplicação contra um PostgreSQL real (Dev Services,
 * que usa Testcontainers), com o schema aplicado pelo Flyway.
 */
@QuarkusTest
class RecursoDeRedesTest {

    @Test
    void criaUmaRedeEDepoisAConsulta() {
        int id = given()
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Cinépolis\"}")
                .when().post("/redes")
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("nome", is("Cinépolis"))
                .extract().path("id");

        given()
                .when().get("/redes/{id}", id)
                .then()
                .statusCode(200)
                .body("nome", is("Cinépolis"));
    }

    @Test
    void respondeNaoEncontradoParaRedeInexistente() {
        given()
                .when().get("/redes/{id}", 999_999)
                .then()
                .statusCode(404);
    }

    @Test
    void criaUmCinemaVinculadoAUmaRede() {
        int redeId = given()
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Rede Natal\"}")
                .when().post("/redes")
                .then().statusCode(201)
                .extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .body("{\"redeId\":" + redeId + ",\"nome\":\"Midway\",\"cidade\":\"Natal\",\"uf\":\"RN\"}")
                .when().post("/cinemas")
                .then()
                .statusCode(201)
                .body("nome", is("Midway"))
                .body("redeId", is(redeId));

        given()
                .queryParam("redeId", redeId)
                .when().get("/cinemas")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].nome", is("Midway"));
    }

    @Test
    void recusaCinemaDeRedeInexistente() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"redeId\":999999,\"nome\":\"Fantasma\",\"cidade\":\"Natal\",\"uf\":\"RN\"}")
                .when().post("/cinemas")
                .then()
                .statusCode(404);
    }
}
