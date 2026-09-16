package br.ufrn.bomfilme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Teste de ponta a ponta: sobe a aplicação contra um PostgreSQL real (Dev Services,
 * que usa Testcontainers), com o schema aplicado pelo Flyway.
 */
@QuarkusTest
class RecursoDeRedesTest {

    /** Os testes compartilham o banco; nomes únicos evitam colisão entre eles. */
    private static String nomeUnico(String prefixo) {
        return prefixo + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private static int criarRede(String nome) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("nome", nome))
                .when().post("/redes")
                .then().statusCode(201)
                .extract().path("id");
    }

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
                .body("criadoEm", notNullValue())
                .extract().path("id");

        given()
                .when().get("/redes/{id}", id)
                .then()
                .statusCode(200)
                .body("nome", is("Cinépolis"));
    }

    @Test
    void listaIncluiARedeCriada() {
        String nome = nomeUnico("Rede listada");
        criarRede(nome);

        given()
                .when().get("/redes")
                .then()
                .statusCode(200)
                .body("nome", hasItem(nome));
    }

    @Test
    void respondeNaoEncontradoParaRedeInexistente() {
        given()
                .when().get("/redes/{id}", 999_999)
                .then()
                .statusCode(404)
                .body("mensagem", notNullValue());
    }

    @Test
    void removeEspacosDasBordasDoNome() {
        String nome = nomeUnico("Rede aparada");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("nome", "   " + nome + "   "))
                .when().post("/redes")
                .then()
                .statusCode(201)
                .body("nome", is(nome));
    }

    @Test
    void recusaNomeDuplicadoComConflito() {
        String nome = nomeUnico("Rede duplicada");
        criarRede(nome);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("nome", nome))
                .when().post("/redes")
                .then()
                .statusCode(409)
                .body("mensagem", notNullValue());
    }

    @Test
    void recusaNomeEmBranco() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("nome", "   "))
                .when().post("/redes")
                .then()
                .statusCode(400);
    }

    @Test
    void recusaNomeAusente() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when().post("/redes")
                .then()
                .statusCode(400);
    }

    @Test
    void recusaNomeMaiorQueOLimiteDaColuna() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("nome", "a".repeat(121)))
                .when().post("/redes")
                .then()
                .statusCode(400);
    }

    @Test
    void recusaRequisicaoSemCorpo() {
        given()
                .contentType(ContentType.JSON)
                .when().post("/redes")
                .then()
                .statusCode(400);
    }
}
