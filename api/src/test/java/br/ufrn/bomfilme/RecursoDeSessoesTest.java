package br.ufrn.bomfilme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Testes de ponta a ponta do recurso /sessoes/{id}/assentos (issue #6). */
@QuarkusTest
class RecursoDeSessoesTest {

    @Inject
    EntityManager em;

    private static String nomeUnico(String prefixo) {
        return prefixo + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Cria rede, cinema e sala numa única transação e devolve a sala persistida. */
    private Sala criarSala() {
        return QuarkusTransaction.requiringNew().call(() -> {
            RedeCinema rede = new RedeCinema(nomeUnico("Rede"));
            em.persist(rede);
            Cinema cinema = new Cinema(rede, nomeUnico("Cinema"), "Natal", "RN");
            em.persist(cinema);
            Sala sala = new Sala(cinema, nomeUnico("Sala"));
            em.persist(sala);
            return sala;
        });
    }

    private Assento criarAssento(Sala sala, String fileira, int numero) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Assento assento = new Assento(sala, fileira, numero);
            em.persist(assento);
            return assento;
        });
    }

    private Sessao criarSessao(Sala sala) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Sessao sessao = new Sessao(sala, 1L, OffsetDateTime.now().plusDays(1));
            em.persist(sessao);
            return sessao;
        });
    }

    private void ocupar(Sessao sessao, Assento assento) {
        QuarkusTransaction.requiringNew().run(() -> em.persist(new Ocupacao(sessao, assento)));
    }

    @Test
    void listaAssentosComDisponibilidadeDaSessao() {
        Sala sala = criarSala();
        Assento livre = criarAssento(sala, "A", 1);
        Assento ocupado = criarAssento(sala, "A", 2);
        Sessao sessao = criarSessao(sala);
        ocupar(sessao, ocupado);

        given()
                .when().get("/sessoes/{id}/assentos", sessao.id)
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("find { it.id == " + livre.id + " }.disponivel", is(true))
                .body("find { it.id == " + ocupado.id + " }.disponivel", is(false));
    }

    @Test
    void naoRetornaAssentosDeOutraSala() {
        Sala sala = criarSala();
        Assento assentoDaSala = criarAssento(sala, "B", 1);
        Sessao sessao = criarSessao(sala);

        Sala outraSala = criarSala();
        criarAssento(outraSala, "B", 1);

        given()
                .when().get("/sessoes/{id}/assentos", sessao.id)
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("id", hasItem(assentoDaSala.id.intValue()));
    }

    @Test
    void respondeNaoEncontradoParaSessaoInexistente() {
        given()
                .when().get("/sessoes/{id}/assentos", 999_999)
                .then()
                .statusCode(404)
                .body("mensagem", notNullValue());
    }

    @Test
    void selecionaUmAssentoDisponivelEDeixaOOutroOcupado() {
        Sala sala = criarSala();
        Assento assento = criarAssento(sala, "C", 1);
        Sessao sessao = criarSessao(sala);

        given()
                .when().post("/sessoes/{id}/assentos/{assentoId}", sessao.id, assento.id)
                .then().statusCode(201);

        given()
                .when().get("/sessoes/{id}/assentos", sessao.id)
                .then()
                .statusCode(200)
                .body("find { it.id == " + assento.id + " }.disponivel", is(false));
    }

    @Test
    void rejeitaSelecionarAssentoJaOcupado() {
        Sala sala = criarSala();
        Assento assento = criarAssento(sala, "C", 2);
        Sessao sessao = criarSessao(sala);
        ocupar(sessao, assento);

        given()
                .when().post("/sessoes/{id}/assentos/{assentoId}", sessao.id, assento.id)
                .then()
                .statusCode(409)
                .body("mensagem", notNullValue());
    }

    @Test
    void rejeitaSelecionarAssentoDeOutraSala() {
        Sala sala = criarSala();
        Sessao sessao = criarSessao(sala);

        Sala outraSala = criarSala();
        Assento assentoDeOutraSala = criarAssento(outraSala, "D", 1);

        given()
                .when().post("/sessoes/{id}/assentos/{assentoId}", sessao.id, assentoDeOutraSala.id)
                .then()
                .statusCode(404)
                .body("mensagem", notNullValue());
    }
}
