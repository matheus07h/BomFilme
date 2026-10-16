package br.ufrn.bomfilme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
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

    @Inject
    EntityManager entityManager;

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

    private static int criarCinema(int redeId, String nome) {
        return criarCinema(cinema(redeId, nome, "Natal", "RN"));
    }

    private static int criarCinema(Map<String, Object> corpo) {
        return given()
                .contentType(ContentType.JSON)
                .body(corpo)
                .when().post("/cinemas")
                .then().statusCode(201)
                .extract().path("id");
    }

    private long criarSala(long cinemaId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Sala sala = new Sala(entityManager.getReference(Cinema.class, cinemaId), "Sala 1");
            entityManager.persist(sala);
            entityManager.flush();
            return sala.id;
        });
    }

    private void removerSala(long salaId) {
        QuarkusTransaction.requiringNew().run(() ->
                entityManager.remove(entityManager.getReference(Sala.class, salaId)));
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
                .body("[0].id", is(cinemaId))
                .body("[0].redeId", is(redeId))
                .body("[0].nome", is("Midway"))
                .body("[0].cidade", is("Natal"))
                .body("[0].uf", is("RN"))
                .body("[0].criadoEm", notNullValue());
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
                .queryParam("nome", nome)
                .when().get("/cinemas")
                .then()
                .statusCode(200)
                .body("nome", hasItem(nome));
    }

    @Test
    void filtraCinemasPorNomeCidadeEUf() {
        int redeId = criarRede();
        String marca = UUID.randomUUID().toString().substring(0, 8);
        int natal = criarCinema(cinema(redeId, "Cine Natal " + marca, "Natal", "RN"));
        int recife = criarCinema(cinema(redeId, "Cine Recife " + marca, "Recife", "PE"));
        criarCinema(cinema(redeId, "Outro " + marca, "Parnamirim", "RN"));

        given()
                .queryParam("redeId", redeId)
                .queryParam("nome", "CINE")
                .when().get("/cinemas")
                .then()
                .statusCode(200)
                .body("id", contains(natal, recife));

        given()
                .queryParam("redeId", redeId)
                .queryParam("uf", "pe")
                .when().get("/cinemas")
                .then()
                .statusCode(200)
                .body("id", contains(recife));

        given()
                .queryParam("redeId", redeId)
                .queryParam("cidade", "nat")
                .when().get("/cinemas")
                .then()
                .statusCode(200)
                .body("id", contains(natal));

        given()
                .queryParam("redeId", redeId)
                .queryParam("uf", "RN")
                .queryParam("nome", "cine")
                .when().get("/cinemas")
                .then()
                .statusCode(200)
                .body("id", contains(natal));

        given()
                .queryParam("redeId", redeId)
                .queryParam("uf", "SP")
                .when().get("/cinemas")
                .then()
                .statusCode(200)
                .body("$", empty());
    }

    @Test
    void recusaFiltrosInvalidosNosCinemas() {
        given().queryParam("uf", "RNN").when().get("/cinemas").then().statusCode(400);
        given().queryParam("uf", "R1").when().get("/cinemas").then().statusCode(400);
        given().queryParam("redeId", 0).when().get("/cinemas").then().statusCode(400);
        given().queryParam("nome", "a".repeat(121)).when().get("/cinemas").then().statusCode(400);
        given().queryParam("cidade", "a".repeat(121)).when().get("/cinemas").then().statusCode(400);
    }

    @Test
    void atualizaUmCinema() {
        int redeId = criarRede();
        int cinemaId = criarCinema(redeId, nomeUnico("Cinema antigo"));
        String novoNome = nomeUnico("Cinema novo");

        given()
                .contentType(ContentType.JSON)
                .body(cinema(redeId, "  " + novoNome + "  ", "  Recife ", "pe"))
                .when().put("/cinemas/{id}", cinemaId)
                .then()
                .statusCode(200)
                .body("id", is(cinemaId))
                .body("redeId", is(redeId))
                .body("nome", is(novoNome))
                .body("cidade", is("Recife"))
                .body("uf", is("PE"));

        given()
                .when().get("/cinemas/{id}", cinemaId)
                .then()
                .statusCode(200)
                .body("nome", is(novoNome))
                .body("cidade", is("Recife"))
                .body("uf", is("PE"));
    }

    @Test
    void moveUmCinemaParaOutraRede() {
        int origem = criarRede();
        int destino = criarRede();
        int cinemaId = criarCinema(origem, nomeUnico("Cinema viajante"));

        given()
                .contentType(ContentType.JSON)
                .body(cinema(destino, nomeUnico("Cinema viajante"), "Natal", "RN"))
                .when().put("/cinemas/{id}", cinemaId)
                .then()
                .statusCode(200)
                .body("redeId", is(destino));

        given()
                .queryParam("redeId", origem)
                .when().get("/cinemas")
                .then()
                .statusCode(200)
                .body("$", empty());

        given()
                .queryParam("redeId", destino)
                .when().get("/cinemas")
                .then()
                .statusCode(200)
                .body("id", contains(cinemaId));
    }

    @Test
    void recusaAtualizarCinemaInexistente() {
        int redeId = criarRede();

        given()
                .contentType(ContentType.JSON)
                .body(cinema(redeId, "Fantasma", "Natal", "RN"))
                .when().put("/cinemas/{id}", 999_999)
                .then()
                .statusCode(404)
                .body("detail", notNullValue());
    }

    @Test
    void recusaMoverCinemaParaRedeInexistente() {
        int redeId = criarRede();
        int cinemaId = criarCinema(redeId, nomeUnico("Cinema fixo"));

        given()
                .contentType(ContentType.JSON)
                .body(cinema(999_999, "Cinema fixo", "Natal", "RN"))
                .when().put("/cinemas/{id}", cinemaId)
                .then()
                .statusCode(404)
                .body("detail", notNullValue());

        given()
                .when().get("/cinemas/{id}", cinemaId)
                .then()
                .statusCode(200)
                .body("redeId", is(redeId));
    }

    @Test
    void recusaAtualizarParaNomeJaUsadoNaRedeDeDestinoComConflito() {
        int redeId = criarRede();
        String nomeExistente = nomeUnico("Cinema existente");
        criarCinema(redeId, nomeExistente);
        int cinemaId = criarCinema(redeId, nomeUnico("Cinema a renomear"));

        given()
                .contentType(ContentType.JSON)
                .body(cinema(redeId, nomeExistente, "Natal", "RN"))
                .when().put("/cinemas/{id}", cinemaId)
                .then()
                .statusCode(409)
                .body("detail", notNullValue());
    }

    @Test
    void atualizacaoComOsMesmosDadosEIdempotente() {
        int redeId = criarRede();
        String nome = nomeUnico("Cinema idempotente");
        int cinemaId = criarCinema(redeId, nome);

        for (int i = 0; i < 2; i++) {
            given()
                    .contentType(ContentType.JSON)
                    .body(cinema(redeId, nome, "Natal", "RN"))
                    .when().put("/cinemas/{id}", cinemaId)
                    .then()
                    .statusCode(200);
        }
    }

    @Test
    void recusaAtualizacaoInvalida() {
        int redeId = criarRede();
        int cinemaId = criarCinema(redeId, nomeUnico("Cinema valido"));

        for (Map<String, Object> corpo : List.of(
                cinema(null, "Cinema", "Natal", "RN"),
                cinema(redeId, " ", "Natal", "RN"),
                cinema(redeId, "Cinema", " ", "RN"),
                cinema(redeId, "Cinema", "Natal", "RNN"),
                cinema(redeId, "a".repeat(121), "Natal", "RN"))) {
            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                    .when().put("/cinemas/{id}", cinemaId)
                    .then()
                    .statusCode(400);
        }

        given()
                .contentType(ContentType.JSON)
                .when().put("/cinemas/{id}", cinemaId)
                .then()
                .statusCode(400);
    }

    @Test
    void removeUmCinemaSemSalas() {
        int redeId = criarRede();
        int cinemaId = criarCinema(redeId, nomeUnico("Cinema removivel"));

        given().when().delete("/cinemas/{id}", cinemaId).then().statusCode(204);

        given().when().get("/cinemas/{id}", cinemaId).then().statusCode(404);
        given().when().delete("/cinemas/{id}", cinemaId).then().statusCode(404);
    }

    @Test
    void recusaRemoverCinemaInexistente() {
        given()
                .when().delete("/cinemas/{id}", 999_999)
                .then()
                .statusCode(404)
                .body("detail", notNullValue());
    }

    @Test
    void impedeRemoverCinemaComSalasVinculadas() {
        int redeId = criarRede();
        int cinemaId = criarCinema(redeId, nomeUnico("Cinema com sala"));
        long salaId = criarSala(cinemaId);

        given()
                .when().delete("/cinemas/{id}", cinemaId)
                .then()
                .statusCode(409)
                .body("detail", notNullValue());

        given().when().get("/cinemas/{id}", cinemaId).then().statusCode(200);

        removerSala(salaId);
        given().when().delete("/cinemas/{id}", cinemaId).then().statusCode(204);
    }

    @Test
    void paginaCinemasFiltradosPorRedeComOrdenacaoEstavel() {
        int redeId = criarRede();
        int primeiroId = criarCinema(redeId, nomeUnico("Primeiro cinema"));
        int segundoId = criarCinema(redeId, nomeUnico("Segundo cinema"));
        int terceiroId = criarCinema(redeId, nomeUnico("Terceiro cinema"));

        given()
                .queryParam("redeId", redeId)
                .queryParam("pagina", 0)
                .queryParam("tamanho", 2)
                .when().get("/cinemas")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].id", is(primeiroId))
                .body("[1].id", is(segundoId));

        given()
                .queryParam("redeId", redeId)
                .queryParam("pagina", 1)
                .queryParam("tamanho", 2)
                .when().get("/cinemas")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].id", is(terceiroId));
    }

    @Test
    void retornaListaVaziaQuandoAPaginaNaoTemCinemas() {
        int redeId = criarRede();
        criarCinema(redeId, nomeUnico("Cinema unico"));

        given()
                .queryParam("redeId", redeId)
                .queryParam("pagina", 1)
                .queryParam("tamanho", 20)
                .when().get("/cinemas")
                .then()
                .statusCode(200)
                .body("$", empty());
    }

    @Test
    void limitaOTamanhoMaximoDaPagina() {
        given()
                .queryParam("tamanho", Paginacao.TAMANHO_MAXIMO + 1)
                .when().get("/cinemas")
                .then()
                .statusCode(400);
    }

    @Test
    void recusaParametrosDePaginacaoInvalidos() {
        given()
                .queryParam("pagina", -1)
                .when().get("/cinemas")
                .then()
                .statusCode(400);

        given()
                .queryParam("tamanho", 0)
                .when().get("/cinemas")
                .then()
                .statusCode(400);
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
                .body("detail", notNullValue());
    }

    @Test
    void recusaCinemaDeRedeInexistente() {
        given()
                .contentType(ContentType.JSON)
                .body(cinema(999_999, "Fantasma", "Natal", "RN"))
                .when().post("/cinemas")
                .then()
                .statusCode(404)
                .body("detail", notNullValue());
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
                .body("detail", notNullValue());
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
