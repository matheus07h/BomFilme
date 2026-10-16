package br.ufrn.bomfilme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

/** Testes de ponta a ponta do CRUD de /sessoes e do conflito de horários na mesma sala (issue #10). */
@QuarkusTest
class ProgramacaoDeSessoesTest {

    private static final AtomicLong PROXIMO_TMDB_ID = new AtomicLong(3_000_000);

    /** Um horário fixo no futuro, em hora cheia, para os cálculos de sobreposição ficarem legíveis. */
    private static final OffsetDateTime AMANHA_AS_14H = OffsetDateTime.now(ZoneOffset.UTC)
            .plusDays(1)
            .truncatedTo(ChronoUnit.DAYS)
            .plusHours(14);

    @Inject
    EntityManager em;

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

    private static int criarSala(int cinemaId) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("cinemaId", cinemaId, "nome", nomeUnico("Sala")))
                .when().post("/salas")
                .then().statusCode(201)
                .extract().path("id");
    }

    private long criarFilme(Integer duracaoMinutos) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Filme filme = new Filme();
            filme.tmdbId = PROXIMO_TMDB_ID.incrementAndGet();
            filme.titulo = nomeUnico("Filme");
            filme.tituloOriginal = filme.titulo;
            filme.sinopse = "Sinopse de teste";
            filme.dataLancamento = LocalDate.of(2020, 1, 1);
            filme.notaMediaTmdb = 7.0;
            filme.duracaoMinutos = duracaoMinutos;
            em.persist(filme);
            return filme.id;
        });
    }

    private static Map<String, Object> sessao(Object salaId, Object filmeId, OffsetDateTime inicio) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("salaId", salaId);
        corpo.put("filmeId", filmeId);
        corpo.put("inicio", inicio == null ? null : inicio.toString());
        return corpo;
    }

    private static int criarSessao(int salaId, long filmeId, OffsetDateTime inicio) {
        return given()
                .contentType(ContentType.JSON)
                .body(sessao(salaId, filmeId, inicio))
                .when().post("/sessoes")
                .then().statusCode(201)
                .extract().path("id");
    }

    private static void postarSessao(int salaId, long filmeId, OffsetDateTime inicio, int status) {
        given().contentType(ContentType.JSON)
                .body(sessao(salaId, filmeId, inicio))
                .when().post("/sessoes")
                .then().statusCode(status);
    }

    @Test
    void criaUmaSessaoComOFimCalculadoPelaDuracaoDoFilme() {
        int salaId = criarSala(criarCinema());
        long filmeId = criarFilme(120);

        int sessaoId = given()
                .contentType(ContentType.JSON)
                .body(sessao(salaId, filmeId, AMANHA_AS_14H))
                .when().post("/sessoes")
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("salaId", is(salaId))
                .body("filmeId", is((int) filmeId))
                .extract().path("id");

        String fim = given().when().get("/sessoes/{id}", sessaoId)
                .then().statusCode(200)
                .body("salaId", is(salaId))
                .extract().path("fim");
        assertTrue(OffsetDateTime.parse(fim).isEqual(AMANHA_AS_14H.plusMinutes(120)));
    }

    @Test
    void recusaSessoesSobrepostasNaMesmaSala() {
        int salaId = criarSala(criarCinema());
        long filmeId = criarFilme(120);
        criarSessao(salaId, filmeId, AMANHA_AS_14H);

        postarSessao(salaId, filmeId, AMANHA_AS_14H, 409);
        postarSessao(salaId, filmeId, AMANHA_AS_14H.plusMinutes(119), 409);
        postarSessao(salaId, filmeId, AMANHA_AS_14H.minusMinutes(119), 409);
        given().contentType(ContentType.JSON)
                .body(sessao(salaId, filmeId, AMANHA_AS_14H.plusHours(1)))
                .when().post("/sessoes")
                .then().statusCode(409)
                .body("mensagem", is("a sala " + salaId + " já possui uma sessão nesse horário"));
    }

    @Test
    void permiteSessoesEmSequenciaOuEmOutraSala() {
        int cinemaId = criarCinema();
        int salaId = criarSala(cinemaId);
        int outraSala = criarSala(cinemaId);
        long filmeId = criarFilme(120);
        criarSessao(salaId, filmeId, AMANHA_AS_14H);

        postarSessao(salaId, filmeId, AMANHA_AS_14H.plusMinutes(120), 201);
        postarSessao(salaId, filmeId, AMANHA_AS_14H.minusMinutes(120), 201);
        postarSessao(outraSala, filmeId, AMANHA_AS_14H, 201);
    }

    @Test
    void oBancoRecusaSobreposicaoMesmoSemAVerificacaoDaAplicacao() {
        long salaId = criarSala(criarCinema());
        long filmeId = criarFilme(120);
        criarSessao((int) salaId, filmeId, AMANHA_AS_14H);

        RuntimeException erro = assertThrows(RuntimeException.class, () ->
                QuarkusTransaction.requiringNew().run(() -> {
                    em.persist(new Sessao(em.getReference(Sala.class, salaId), filmeId,
                            AMANHA_AS_14H.plusMinutes(30), AMANHA_AS_14H.plusMinutes(150)));
                    em.flush();
                }));
        ConstraintViolationException violacao = procurarViolacao(erro);
        assertNotNull(violacao);
        assertEquals(MapeadorDeErrosDePersistencia.VIOLACAO_DE_EXCLUSAO, violacao.getSQLState());
    }

    @Test
    void atualizaUmaSessaoSemConflitarConsigoMesma() {
        int salaId = criarSala(criarCinema());
        long filmeId = criarFilme(120);
        long outroFilme = criarFilme(90);
        int sessaoId = criarSessao(salaId, filmeId, AMANHA_AS_14H);
        criarSessao(salaId, filmeId, AMANHA_AS_14H.plusHours(4));

        String fim = given().contentType(ContentType.JSON)
                .body(sessao(salaId, outroFilme, AMANHA_AS_14H.plusMinutes(30)))
                .when().put("/sessoes/{id}", sessaoId)
                .then().statusCode(200)
                .body("filmeId", is((int) outroFilme))
                .extract().path("fim");
        assertTrue(OffsetDateTime.parse(fim).isEqual(AMANHA_AS_14H.plusMinutes(120)));

        given().contentType(ContentType.JSON)
                .body(sessao(salaId, filmeId, AMANHA_AS_14H.plusHours(3)))
                .when().put("/sessoes/{id}", sessaoId)
                .then().statusCode(409);
    }

    @Test
    void listaAsSessoesComFiltrosEmOrdemDeInicio() {
        int cinemaId = criarCinema();
        int salaId = criarSala(cinemaId);
        int outraSala = criarSala(cinemaId);
        long filmeId = criarFilme(100);
        long outroFilme = criarFilme(100);
        int tarde = criarSessao(salaId, filmeId, AMANHA_AS_14H.plusHours(4));
        int cedo = criarSessao(salaId, outroFilme, AMANHA_AS_14H);
        int outra = criarSessao(outraSala, filmeId, AMANHA_AS_14H);

        given().queryParam("salaId", salaId).when().get("/sessoes")
                .then().statusCode(200)
                .body("id", contains(cedo, tarde));
        given().queryParam("cinemaId", cinemaId).queryParam("filmeId", filmeId).when().get("/sessoes")
                .then().statusCode(200)
                .body("id", contains(outra, tarde));
        given().queryParam("cinemaId", cinemaId).queryParam("tamanho", 1).queryParam("pagina", 2)
                .when().get("/sessoes")
                .then().statusCode(200)
                .body("id", contains(tarde));
        given().queryParam("salaId", 0).when().get("/sessoes").then().statusCode(400);
        given().queryParam("tamanho", 101).when().get("/sessoes").then().statusCode(400);
    }

    @Test
    void validaAEntradaDaSessao() {
        int salaId = criarSala(criarCinema());
        long filmeId = criarFilme(120);

        postarSessao(salaId, filmeId, OffsetDateTime.now().minusHours(1), 400);
        postarSessao(salaId, filmeId, null, 400);
        given().contentType(ContentType.JSON).body(sessao(null, filmeId, AMANHA_AS_14H))
                .when().post("/sessoes").then().statusCode(400);
        given().contentType(ContentType.JSON).body(sessao(salaId, -1, AMANHA_AS_14H))
                .when().post("/sessoes").then().statusCode(400);
        given().contentType(ContentType.JSON)
                .when().post("/sessoes").then().statusCode(400);
    }

    @Test
    void recusaSalaOuFilmeInexistenteEFilmeSemDuracao() {
        int salaId = criarSala(criarCinema());
        long filmeId = criarFilme(120);
        long semDuracao = criarFilme(null);

        given().contentType(ContentType.JSON).body(sessao(999_999_999, filmeId, AMANHA_AS_14H))
                .when().post("/sessoes")
                .then().statusCode(404)
                .body("mensagem", is("sala 999999999 não encontrada"));
        given().contentType(ContentType.JSON).body(sessao(salaId, 999_999_999, AMANHA_AS_14H))
                .when().post("/sessoes")
                .then().statusCode(404)
                .body("mensagem", is("filme 999999999 não encontrado"));
        given().contentType(ContentType.JSON).body(sessao(salaId, semDuracao, AMANHA_AS_14H))
                .when().post("/sessoes")
                .then().statusCode(422)
                .body("mensagem", is("filme " + semDuracao + " não tem duração cadastrada"));
        given().contentType(ContentType.JSON).body(sessao(salaId, filmeId, AMANHA_AS_14H))
                .when().put("/sessoes/{id}", 999_999_999)
                .then().statusCode(404);
    }

    @Test
    void removeSessaoSemAssentosOcupadosEImpedeComOcupacao() {
        int salaId = criarSala(criarCinema());
        long filmeId = criarFilme(120);
        int livre = criarSessao(salaId, filmeId, AMANHA_AS_14H);
        int ocupada = criarSessao(salaId, filmeId, AMANHA_AS_14H.plusHours(3));
        int assentoId = given().contentType(ContentType.JSON)
                .body(Map.of("fileira", "A", "numero", 1))
                .when().post("/salas/{id}/assentos", salaId)
                .then().statusCode(201)
                .extract().path("id");
        given().when().post("/sessoes/{id}/assentos/{assentoId}", ocupada, assentoId).then().statusCode(201);

        given().when().delete("/sessoes/{id}", livre).then().statusCode(204);
        given().when().get("/sessoes/{id}", livre).then().statusCode(404);
        given().when().delete("/sessoes/{id}", livre).then().statusCode(404);
        given().when().delete("/sessoes/{id}", ocupada)
                .then().statusCode(409)
                .body("mensagem", is("sessão " + ocupada + " possui assentos ocupados e não pode ser removida"));
    }

    private static ConstraintViolationException procurarViolacao(Throwable erro) {
        for (Throwable atual = erro; atual != null; atual = atual.getCause()) {
            if (atual instanceof ConstraintViolationException violacao) {
                return violacao;
            }
        }
        return null;
    }
}
