package br.ufrn.bomfilme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
                .queryParam("nome", nome)
                .when().get("/redes")
                .then()
                .statusCode(200)
                .body("nome", hasItem(nome));
    }

    @Test
    void paginaRedesNoBancoComOrdenacaoEstavel() {
        String marca = UUID.randomUUID().toString().substring(0, 8);
        int primeira = criarRede("Rede A " + marca);
        int segunda = criarRede("Rede B " + marca);
        int terceira = criarRede("Rede C " + marca);

        given()
                .queryParam("nome", marca)
                .queryParam("pagina", 0)
                .queryParam("tamanho", 2)
                .when().get("/redes")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].id", is(primeira))
                .body("[1].id", is(segunda));

        given()
                .queryParam("nome", marca)
                .queryParam("pagina", 1)
                .queryParam("tamanho", 2)
                .when().get("/redes")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].id", is(terceira));

        given()
                .queryParam("nome", marca)
                .queryParam("pagina", 2)
                .queryParam("tamanho", 2)
                .when().get("/redes")
                .then()
                .statusCode(200)
                .body("$", empty());
    }

    @Test
    void filtraRedesPeloNomeSemDiferenciarMaiusculasENemInterpretarCoringas() {
        String marca = UUID.randomUUID().toString().substring(0, 8);
        criarRede("Cine_Filtro " + marca);
        criarRede("CineXFiltro " + marca);

        given()
                .queryParam("nome", "cine_filtro " + marca.toUpperCase())
                .when().get("/redes")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].nome", is("Cine_Filtro " + marca));

        given()
                .queryParam("nome", "%")
                .queryParam("tamanho", 100)
                .when().get("/redes")
                .then()
                .statusCode(200)
                .body("nome", not(hasItem("CineXFiltro " + marca)));
    }

    @Test
    void limitaOTamanhoMaximoDaPaginaDeRedes() {
        given()
                .queryParam("tamanho", Paginacao.TAMANHO_MAXIMO + 1)
                .when().get("/redes")
                .then()
                .statusCode(400);
    }

    @Test
    void recusaParametrosDeListagemInvalidosNasRedes() {
        given().queryParam("pagina", -1).when().get("/redes").then().statusCode(400);
        given().queryParam("tamanho", 0).when().get("/redes").then().statusCode(400);
        given().queryParam("nome", "a".repeat(121)).when().get("/redes").then().statusCode(400);
    }

    @Test
    void atualizaUmaRede() {
        int id = criarRede(nomeUnico("Rede antiga"));
        String novoNome = nomeUnico("Rede nova");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("nome", "  " + novoNome + "  "))
                .when().put("/redes/{id}", id)
                .then()
                .statusCode(200)
                .body("id", is(id))
                .body("nome", is(novoNome));

        given()
                .when().get("/redes/{id}", id)
                .then()
                .statusCode(200)
                .body("nome", is(novoNome));
    }

    @Test
    void atualizacaoComOMesmoNomeDaPropriaRedeEIdempotente() {
        String nome = nomeUnico("Rede idempotente");
        int id = criarRede(nome);

        for (int i = 0; i < 2; i++) {
            given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("nome", nome))
                    .when().put("/redes/{id}", id)
                    .then()
                    .statusCode(200)
                    .body("nome", is(nome));
        }
    }

    @Test
    void recusaAtualizarRedeInexistente() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("nome", "Qualquer"))
                .when().put("/redes/{id}", 999_999)
                .then()
                .statusCode(404)
                .body("mensagem", notNullValue());
    }

    @Test
    void recusaAtualizarParaNomeDeOutraRedeComConflito() {
        String nomeExistente = nomeUnico("Rede existente");
        criarRede(nomeExistente);
        int id = criarRede(nomeUnico("Rede a renomear"));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("nome", nomeExistente))
                .when().put("/redes/{id}", id)
                .then()
                .statusCode(409)
                .body("mensagem", notNullValue());
    }

    @Test
    void recusaAtualizacaoInvalida() {
        int id = criarRede(nomeUnico("Rede valida"));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("nome", "  "))
                .when().put("/redes/{id}", id)
                .then()
                .statusCode(400);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("nome", "a".repeat(121)))
                .when().put("/redes/{id}", id)
                .then()
                .statusCode(400);

        given()
                .contentType(ContentType.JSON)
                .when().put("/redes/{id}", id)
                .then()
                .statusCode(400);
    }

    @Test
    void removeUmaRedeSemCinemas() {
        int id = criarRede(nomeUnico("Rede removivel"));

        given().when().delete("/redes/{id}", id).then().statusCode(204);

        given().when().get("/redes/{id}", id).then().statusCode(404);
        given().when().delete("/redes/{id}", id).then().statusCode(404);
    }

    @Test
    void recusaRemoverRedeInexistente() {
        given()
                .when().delete("/redes/{id}", 999_999)
                .then()
                .statusCode(404)
                .body("mensagem", notNullValue());
    }

    @Test
    void impedeRemoverRedeComCinemasVinculados() {
        int redeId = criarRede(nomeUnico("Rede com cinema"));
        int cinemaId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("redeId", redeId, "nome", "Cinema vinculado", "cidade", "Natal", "uf", "RN"))
                .when().post("/cinemas")
                .then().statusCode(201)
                .extract().path("id");

        given()
                .when().delete("/redes/{id}", redeId)
                .then()
                .statusCode(409)
                .body("mensagem", notNullValue());

        given().when().get("/redes/{id}", redeId).then().statusCode(200);
        given().when().get("/cinemas/{id}", cinemaId).then().statusCode(200);

        given().when().delete("/cinemas/{id}", cinemaId).then().statusCode(204);
        given().when().delete("/redes/{id}", redeId).then().statusCode(204);
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
