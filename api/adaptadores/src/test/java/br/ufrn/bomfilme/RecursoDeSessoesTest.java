package br.ufrn.bomfilme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import br.ufrn.bomfilme.dominio.Assento;
import br.ufrn.bomfilme.dominio.Sala;
import br.ufrn.bomfilme.dominio.Sessao;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

/** Testes de ponta a ponta do recurso /sessoes/{id}/assentos (issue #6). */
@QuarkusTest
class RecursoDeSessoesTest {

    @Inject
    Fixtures dados;

    @Test
    void listaAssentosComDisponibilidadeDaSessao() {
        Sala sala = dados.sala();
        Assento livre = dados.assento(sala, "A", 1);
        Assento ocupado = dados.assento(sala, "A", 2);
        Sessao sessao = dados.sessao(sala);
        dados.ocupar(sessao, ocupado);

        given()
                .when().get("/sessoes/{id}/assentos", sessao.id())
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("find { it.id == " + livre.id() + " }.disponivel", is(true))
                .body("find { it.id == " + ocupado.id() + " }.disponivel", is(false));
    }

    @Test
    void naoRetornaAssentosDeOutraSala() {
        Sala sala = dados.sala();
        Assento assentoDaSala = dados.assento(sala, "B", 1);
        Sessao sessao = dados.sessao(sala);

        Sala outraSala = dados.sala();
        dados.assento(outraSala, "B", 1);

        given()
                .when().get("/sessoes/{id}/assentos", sessao.id())
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("id", hasItem(assentoDaSala.id().intValue()));
    }

    @Test
    void respondeNaoEncontradoParaSessaoInexistente() {
        given()
                .when().get("/sessoes/{id}/assentos", 999_999)
                .then()
                .statusCode(404)
                .body("detail", notNullValue());
    }

    @Test
    void selecionaUmAssentoDisponivelEDeixaOOutroOcupado() {
        Sala sala = dados.sala();
        Assento assento = dados.assento(sala, "C", 1);
        Sessao sessao = dados.sessao(sala);

        given()
                .when().post("/sessoes/{id}/assentos/{assentoId}", sessao.id(), assento.id())
                .then().statusCode(201);

        given()
                .when().get("/sessoes/{id}/assentos", sessao.id())
                .then()
                .statusCode(200)
                .body("find { it.id == " + assento.id() + " }.disponivel", is(false));
    }

    @Test
    void rejeitaSelecionarAssentoJaOcupado() {
        Sala sala = dados.sala();
        Assento assento = dados.assento(sala, "C", 2);
        Sessao sessao = dados.sessao(sala);
        dados.ocupar(sessao, assento);

        given()
                .when().post("/sessoes/{id}/assentos/{assentoId}", sessao.id(), assento.id())
                .then()
                .statusCode(409)
                .body("detail", notNullValue());
    }

    @Test
    void rejeitaSelecionarAssentoDeOutraSala() {
        Sala sala = dados.sala();
        Sessao sessao = dados.sessao(sala);

        Sala outraSala = dados.sala();
        Assento assentoDeOutraSala = dados.assento(outraSala, "D", 1);

        given()
                .when().post("/sessoes/{id}/assentos/{assentoId}", sessao.id(), assentoDeOutraSala.id())
                .then()
                .statusCode(404)
                .body("detail", notNullValue());
    }
}
