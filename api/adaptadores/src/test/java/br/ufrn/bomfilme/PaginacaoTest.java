package br.ufrn.bomfilme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Toda listagem usa o mesmo envelope de paginação.
 *
 * <p>Antes, só {@code /filmes} devolvia {@code Pagina}; as outras devolviam um array cru,
 * e o cliente não tinha como saber quantas páginas existem. Este teste existe para o
 * próximo recurso não reintroduzir o array.
 */
@QuarkusTest
class PaginacaoTest {

    static Stream<String> listagens() {
        return Stream.of("/redes", "/cinemas", "/salas", "/sessoes", "/filmes");
    }

    @ParameterizedTest
    @MethodSource("listagens")
    void aListagemDevolveOEnvelopeDePaginacao(String rota) {
        given()
                .when().get(rota)
                .then()
                .statusCode(200)
                .body("$", hasKey("itens"))
                .body("$", hasKey("pagina"))
                .body("$", hasKey("tamanho"))
                .body("$", hasKey("total"))
                .body("$", hasKey("totalPaginas"))
                .body("total", greaterThanOrEqualTo(0))
                .body("totalPaginas", greaterThanOrEqualTo(0));
    }

    @ParameterizedTest
    @MethodSource("listagens")
    void aPaginaEOTamanhoPedidosVoltamNaResposta(String rota) {
        given()
                .queryParam("pagina", 0)
                .queryParam("tamanho", 5)
                .when().get(rota)
                .then()
                .statusCode(200)
                .body("pagina", is(0))
                .body("tamanho", is(5));
    }

    @ParameterizedTest
    @MethodSource("listagens")
    void umaPaginaAlemDoFimTrazItensVazioSemPerderOTotal(String rota) {
        given()
                .queryParam("pagina", 9_999)
                .when().get(rota)
                .then()
                .statusCode(200)
                .body("itens", empty())
                .body("total", greaterThanOrEqualTo(0));
    }

    @ParameterizedTest
    @MethodSource("listagens")
    void oTetoDeTamanhoValeParaTodasAsListagens(String rota) {
        given()
                .queryParam("tamanho", 101)
                .when().get(rota)
                .then()
                .statusCode(400)
                .body("type", is("urn:bomfilme:erro:entrada-invalida"));
    }
}
