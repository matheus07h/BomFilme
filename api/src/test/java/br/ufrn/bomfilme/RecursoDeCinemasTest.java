package br.ufrn.bomfilme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes de ponta a ponta do recurso /cinemas contra um PostgreSQL real
 * (Dev Services), cobrindo o caminho feliz, a validação da entrada e as
 * restrições de unicidade definidas na migração.
 */
@QuarkusTest
class RecursoDeCinemasTest {

    private static String nomeUnico(String prefixo) {
        return prefixo + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private static int criarRede() {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("nome", nomeUnico("Rede")))
                .when().post("/redes")
                .then().statusCode(201)
                .extract().path("id");
    }

    private static Map<String, Object> cinema(Object redeId, String nome, String cidade, String uf) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("redeId", redeId);
        corpo.put("nome", nome);
        corpo.put("cidade", cidade);
        corpo.put("uf", uf);
        return corpo;
    }

    @Test
    void criaUmCinemaVinculadoAUmaRede() {
        int redeId = criarRede();

        int cinemaId = given()
                .contentType(ContentType.JSON)
                .body(cinema(redeId, "Midway", "Natal", "RN"))
                .when().post("/cinemas")
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("nome", is("Midway"))
                .body("redeId", is(redeId))
                .body("criadoEm", notNullValue())
                .extract().path("id");

        given()
                .when().get("/cinemas/{id}", cinemaId)
                .then()
                .statusCode(200)
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
    void listaSemFiltroIncluiOCinemaCriado() {
        int redeId = criarRede();
        String nome = nomeUnico("Cinema listado");

        given()
                .contentType(ContentType.JSON)
                .body(cinema(redeId, nome, "Mossoró", "RN"))
                .when().post("/cinemas")
                .then().statusCode(201);

        given()
                .when().get("/cinemas")
                .then()
                .statusCode(200)
                .body("nome", hasItem(nome));
    }

    @Test
    void normalizaUfEAparaEspacos() {
        int redeId = criarRede();

        given()
                .contentType(ContentType.JSON)
                .body(cinema(redeId, "  Praia Shopping  ", "  Natal ", "rn"))
                .when().post("/cinemas")
                .then()
                .statusCode(201)
                .body("nome", is("Praia Shopping"))
                .body("cidade", is("Natal"))
                .body("uf", is("RN"));
    }

    @Test
    void respondeNaoEncontradoParaCinemaInexistente() {
        given()
                .when().get("/cinemas/{id}", 999_999)
                .then()
                .statusCode(404)
                .body("mensagem", notNullValue());
    }

    @Test
    void recusaCinemaDeRedeInexistente() {
        given()
                .contentType(ContentType.JSON)
                .body(cinema(999_999, "Fantasma", "Natal", "RN"))
                .when().post("/cinemas")
                .then()
                .statusCode(404)
                .body("mensagem", notNullValue());
    }

    /** Antes da validação, redeId ausente causava NullPointerException e resposta 500. */
    @Test
    void recusaCinemaSemRedeComRequisicaoInvalida() {
        given()
                .contentType(ContentType.JSON)
                .body(cinema(null, "Sem rede", "Natal", "RN"))
                .when().post("/cinemas")
                .then()
                .statusCode(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "R", "RNN", "R1", "  "})
    void recusaUfInvalida(String uf) {
        int redeId = criarRede();

        given()
                .contentType(ContentType.JSON)
                .body(cinema(redeId, nomeUnico("Cinema"), "Natal", uf))
                .when().post("/cinemas")
                .then()
                .statusCode(400);
    }

    @Test
    void recusaNomeEmBranco() {
        int redeId = criarRede();

        given()
                .contentType(ContentType.JSON)
                .body(cinema(redeId, " ", "Natal", "RN"))
                .when().post("/cinemas")
                .then()
                .statusCode(400);
    }

    @Test
    void recusaNomeRepetidoNaMesmaRedeComConflito() {
        int redeId = criarRede();

        given()
                .contentType(ContentType.JSON)
                .body(cinema(redeId, "Cinemark Natal", "Natal", "RN"))
                .when().post("/cinemas")
                .then().statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(cinema(redeId, "Cinemark Natal", "Natal", "RN"))
                .when().post("/cinemas")
                .then()
                .statusCode(409)
                .body("mensagem", notNullValue());
    }

    @Test
    void permiteOMesmoNomeEmRedesDiferentes() {
        int primeiraRede = criarRede();
        int segundaRede = criarRede();

        given()
                .contentType(ContentType.JSON)
                .body(cinema(primeiraRede, "Cine Centro", "Natal", "RN"))
                .when().post("/cinemas")
                .then().statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(cinema(segundaRede, "Cine Centro", "Natal", "RN"))
                .when().post("/cinemas")
                .then().statusCode(201);
    }
}
