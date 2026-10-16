package br.ufrn.bomfilme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import br.ufrn.bomfilme.dominio.RepositorioDeAssentos;
import br.ufrn.bomfilme.dominio.RepositorioDeSessoes;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** Testes de ponta a ponta de /salas e /salas/{id}/assentos (issue #10). */
@QuarkusTest
class RecursoDeSalasTest {

    private static final AtomicLong PROXIMO_TMDB_ID = new AtomicLong(2_000_000);

    @Inject
    Fixtures dados;

    @Inject
    RepositorioDeSessoes sessoes;

    @Inject
    RepositorioDeAssentos assentos;

    private static String nomeUnico(String prefixo) {
        return prefixo + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private static int criarCinema() {
        int redeId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("nome", nomeUnico("Rede")))
                .when().post("/redes")
                .then().statusCode(201)
                .extract().path("id");
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("redeId", redeId, "nome", nomeUnico("Cinema"), "cidade", "Natal", "uf", "RN"))
                .when().post("/cinemas")
                .then().statusCode(201)
                .extract().path("id");
    }

    private static Map<String, Object> sala(Object cinemaId, String nome) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("cinemaId", cinemaId);
        corpo.put("nome", nome);
        return corpo;
    }

    private static int criarSala(int cinemaId, String nome) {
        return given()
                .contentType(ContentType.JSON)
                .body(sala(cinemaId, nome))
                .when().post("/salas")
                .then().statusCode(201)
                .extract().path("id");
    }

    private static int criarAssento(int salaId, String fileira, int numero) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("fileira", fileira, "numero", numero))
                .when().post("/salas/{id}/assentos", salaId)
                .then().statusCode(201)
                .extract().path("id");
    }

    private long criarSessao(long salaId) {
        OffsetDateTime inicio = OffsetDateTime.now().plusDays(1);
        return sessoes.criar(salaId, dados.filme(100).id(), inicio, inicio.plusMinutes(100)).id();
    }

    private void ocupar(long sessaoId, long assentoId) {
        assentos.ocupar(sessaoId, assentoId);
    }

    @Test
    void criaUmaSalaVinculadaAoCinemaEDepoisAConsulta() {
        int cinemaId = criarCinema();

        int salaId = given()
                .contentType(ContentType.JSON)
                .body(sala(cinemaId, "  Sala 1  "))
                .when().post("/salas")
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("nome", is("Sala 1"))
                .body("cinemaId", is(cinemaId))
                .body("criadoEm", notNullValue())
                .extract().path("id");

        given().when().get("/salas/{id}", salaId)
                .then().statusCode(200)
                .body("nome", is("Sala 1"))
                .body("cinemaId", is(cinemaId));
    }

    @Test
    void filtraEPaginaAsSalasDoCinema() {
        int cinemaId = criarCinema();
        int primeira = criarSala(cinemaId, "Sala 1");
        int segunda = criarSala(cinemaId, "Sala 2");
        int vip = criarSala(cinemaId, "VIP");
        criarSala(criarCinema(), "Sala 1");

        given().queryParam("cinemaId", cinemaId).queryParam("tamanho", 2)
                .when().get("/salas")
                .then().statusCode(200)
                .body("id", contains(primeira, segunda));
        given().queryParam("cinemaId", cinemaId).queryParam("tamanho", 2).queryParam("pagina", 1)
                .when().get("/salas")
                .then().statusCode(200)
                .body("id", contains(vip));
        given().queryParam("cinemaId", cinemaId).queryParam("nome", "vi")
                .when().get("/salas")
                .then().statusCode(200)
                .body("id", contains(vip));
    }

    @Test
    void recusaParametrosDeListagemInvalidos() {
        given().queryParam("cinemaId", 0).when().get("/salas").then().statusCode(400);
        given().queryParam("tamanho", 101).when().get("/salas").then().statusCode(400);
        given().queryParam("pagina", -1).when().get("/salas").then().statusCode(400);
    }

    @Test
    void atualizaUmaSalaEMoveParaOutroCinema() {
        int salaId = criarSala(criarCinema(), "Sala 1");
        int outroCinema = criarCinema();

        given().contentType(ContentType.JSON)
                .body(sala(outroCinema, "Sala 3D"))
                .when().put("/salas/{id}", salaId)
                .then().statusCode(200)
                .body("nome", is("Sala 3D"))
                .body("cinemaId", is(outroCinema));
    }

    @Test
    void recusaSalaInvalidaOuDeCinemaInexistente() {
        given().contentType(ContentType.JSON).body(sala(null, "Sala"))
                .when().post("/salas").then().statusCode(400);
        given().contentType(ContentType.JSON).body(sala(criarCinema(), " "))
                .when().post("/salas").then().statusCode(400);
        given().contentType(ContentType.JSON).body(sala(criarCinema(), "a".repeat(61)))
                .when().post("/salas").then().statusCode(400);
        given().contentType(ContentType.JSON).body(sala(999_999_999, "Sala"))
                .when().post("/salas")
                .then().statusCode(404)
                .body("detail", is("cinema 999999999 não encontrado"));
        given().contentType(ContentType.JSON).body(sala(criarCinema(), "Sala"))
                .when().put("/salas/{id}", 999_999_999)
                .then().statusCode(404);
    }

    @Test
    void recusaNomeDeSalaRepetidoNoMesmoCinema() {
        int cinemaId = criarCinema();
        criarSala(cinemaId, "Sala 1");

        given().contentType(ContentType.JSON).body(sala(cinemaId, "Sala 1"))
                .when().post("/salas")
                .then().statusCode(409);
    }

    @Test
    void removeSalaSemVinculosEImpedeRemoverComAssentosOuSessoes() {
        int livre = criarSala(criarCinema(), "Livre");
        given().when().delete("/salas/{id}", livre).then().statusCode(204);
        given().when().get("/salas/{id}", livre).then().statusCode(404);
        given().when().delete("/salas/{id}", livre).then().statusCode(404);

        int comAssento = criarSala(criarCinema(), "Com assento");
        criarAssento(comAssento, "A", 1);
        given().when().delete("/salas/{id}", comAssento)
                .then().statusCode(409)
                .body("detail", is("sala " + comAssento + " possui assentos vinculados e não pode ser removida"));

        int comSessao = criarSala(criarCinema(), "Com sessão");
        criarSessao(comSessao);
        given().when().delete("/salas/{id}", comSessao)
                .then().statusCode(409)
                .body("detail", is("sala " + comSessao + " possui sessões vinculadas e não pode ser removida"));
    }

    @Test
    void gerenciaOsAssentosDaSala() {
        int salaId = criarSala(criarCinema(), "Sala 1");

        int assentoId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("fileira", "b", "numero", 2))
                .when().post("/salas/{id}/assentos", salaId)
                .then().statusCode(201)
                .header("Location", notNullValue())
                .body("fileira", is("B"))
                .body("numero", is(2))
                .body("salaId", is(salaId))
                .extract().path("id");
        int a1 = criarAssento(salaId, "A", 1);
        int a2 = criarAssento(salaId, "A", 2);

        given().when().get("/salas/{id}/assentos", salaId)
                .then().statusCode(200)
                .body("id", contains(a1, a2, assentoId));
        given().queryParam("fileira", "a").queryParam("tamanho", 1).queryParam("pagina", 1)
                .when().get("/salas/{id}/assentos", salaId)
                .then().statusCode(200)
                .body("id", contains(a2));

        given().contentType(ContentType.JSON)
                .body(Map.of("fileira", "C", "numero", 10))
                .when().put("/salas/{salaId}/assentos/{id}", salaId, assentoId)
                .then().statusCode(200)
                .body("fileira", is("C"))
                .body("numero", is(10));
        given().when().get("/salas/{salaId}/assentos/{id}", salaId, assentoId)
                .then().statusCode(200)
                .body("fileira", is("C"));

        given().when().delete("/salas/{salaId}/assentos/{id}", salaId, assentoId).then().statusCode(204);
        given().when().get("/salas/{salaId}/assentos/{id}", salaId, assentoId).then().statusCode(404);
    }

    @Test
    void mantemCadaAssentoNaSuaSala() {
        int salaId = criarSala(criarCinema(), "Sala 1");
        int outraSala = criarSala(criarCinema(), "Sala 2");
        int assentoId = criarAssento(salaId, "A", 1);

        given().when().get("/salas/{salaId}/assentos/{id}", outraSala, assentoId).then().statusCode(404);
        given().contentType(ContentType.JSON).body(Map.of("fileira", "A", "numero", 2))
                .when().put("/salas/{salaId}/assentos/{id}", outraSala, assentoId)
                .then().statusCode(404);
        given().when().delete("/salas/{salaId}/assentos/{id}", outraSala, assentoId).then().statusCode(404);
        given().when().get("/salas/{id}/assentos", 999_999_999).then().statusCode(404);
        given().contentType(ContentType.JSON).body(Map.of("fileira", "A", "numero", 1))
                .when().post("/salas/{id}/assentos", 999_999_999)
                .then().statusCode(404);
    }

    @Test
    void recusaAssentoInvalidoOuRepetido() {
        int salaId = criarSala(criarCinema(), "Sala 1");
        criarAssento(salaId, "A", 1);

        given().contentType(ContentType.JSON).body(Map.of("fileira", "A", "numero", 1))
                .when().post("/salas/{id}/assentos", salaId)
                .then().statusCode(409);
        given().contentType(ContentType.JSON).body(Map.of("fileira", "ABC", "numero", 1))
                .when().post("/salas/{id}/assentos", salaId)
                .then().statusCode(400);
        given().contentType(ContentType.JSON).body(Map.of("fileira", "1", "numero", 1))
                .when().post("/salas/{id}/assentos", salaId)
                .then().statusCode(400);
        given().contentType(ContentType.JSON).body(Map.of("fileira", "A", "numero", 0))
                .when().post("/salas/{id}/assentos", salaId)
                .then().statusCode(400);
        given().contentType(ContentType.JSON).body(Map.of("fileira", "A"))
                .when().post("/salas/{id}/assentos", salaId)
                .then().statusCode(400);
        given().queryParam("fileira", "1").when().get("/salas/{id}/assentos", salaId).then().statusCode(400);
    }

    @Test
    void impedeRemoverAssentoOcupadoEmSessao() {
        int salaId = criarSala(criarCinema(), "Sala 1");
        int assentoId = criarAssento(salaId, "A", 1);
        long sessaoId = criarSessao(salaId);
        ocupar(sessaoId, assentoId);

        given().when().delete("/salas/{salaId}/assentos/{id}", salaId, assentoId)
                .then().statusCode(409)
                .body("detail", is("assento " + assentoId + " está ocupado em alguma sessão e não pode ser removido"));
    }
}
