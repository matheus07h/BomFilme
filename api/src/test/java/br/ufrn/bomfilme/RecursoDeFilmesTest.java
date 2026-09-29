package br.ufrn.bomfilme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

// IMPORTS DO MOCKITO QUE FALTAVAM:
import static org.mockito.Mockito.*;

import br.ufrn.bomfilme.dtos.response.CreditosTMDBResponse;
import br.ufrn.bomfilme.dtos.response.EquipeResponse;
import br.ufrn.bomfilme.dtos.response.FilmeTMDBResponse;
import br.ufrn.bomfilme.dtos.response.GeneroTMDBResponse;
import br.ufrn.bomfilme.dtos.response.ListarFilmesTMDBResponse;
import br.ufrn.bomfilme.dtos.response.MembroElencoResponse;
import br.ufrn.bomfilme.dtos.response.ResumoFilmeTMDBResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

/**
 * Testes de ponta a ponta do recurso /filmes.
 */
@QuarkusTest
public class RecursoDeFilmesTest {

    private static final AtomicLong PROXIMO_TMDB_ID = new AtomicLong(900_000);

    @InjectMock
    @RestClient
    ClienteTMDB clienteTMDB;

    private long tmdbIdUnico() {
        return PROXIMO_TMDB_ID.incrementAndGet();
    }

    private FilmeTMDBResponse filmeTmdb(long tmdbId, String titulo, LocalDate dataLancamento, CreditosTMDBResponse credits) {
        return new FilmeTMDBResponse(
                tmdbId,
                titulo,
                titulo,
                "Sinopse de " + titulo,
                120,
                "/poster.jpg",
                dataLancamento,
                7.5,
                List.of(new GeneroTMDBResponse(28L, "Ação")),
                credits,
                42.0
        );
    }

    private CreditosTMDBResponse creditos() {
        return new CreditosTMDBResponse(
                List.of(new MembroElencoResponse(1L, "Ator Principal", "Herói", 0),
                        new MembroElencoResponse(2L, "Ator Secundário", "Vilão", 1)),
                List.of(new EquipeResponse(9L, "Diretor Exemplo", "Director"))
        );
    }

    private int importarBasico(long tmdbId, String titulo) {
        // CORRIGIDO: usando isNull() em vez de eq(null)
        when(clienteTMDB.buscarPorId(eq(tmdbId), isNull()))
                .thenReturn(filmeTmdb(tmdbId, titulo, LocalDate.of(2020, 1, 1), null));

        return given()
                .when().post("/filmes/importar-basico/{tmdbId}", tmdbId)
                .then().statusCode(200)
                .extract().path("id");
    }

    @Test
    void importaFilmeBasicoComSucesso() {
        long tmdbId = tmdbIdUnico();
        when(clienteTMDB.buscarPorId(eq(tmdbId), isNull()))
                .thenReturn(filmeTmdb(tmdbId, "Filme Básico", LocalDate.of(2020, 5, 10), null));

        given()
                .when().post("/filmes/importar-basico/{tmdbId}", tmdbId)
                .then()
                .statusCode(200)
                .body("titulo", is("Filme Básico"))
                .body("generos", hasItem("Ação"))
                .body("diretores.size()", is(0))
                .body("elenco.size()", is(0));
    }

    @Test
    void importaFilmeCompletoComElencoEDiretores() {
        long tmdbId = tmdbIdUnico();
        when(clienteTMDB.buscarPorId(eq(tmdbId), eq("credits")))
                .thenReturn(filmeTmdb(tmdbId, "Filme Completo", LocalDate.of(2019, 3, 1), creditos()));

        given()
                .when().post("/filmes/importar-completo/{tmdbId}", tmdbId)
                .then()
                .statusCode(200)
                .body("titulo", is("Filme Completo"))
                .body("diretores", hasItem("Diretor Exemplo"))
                .body("elenco.size()", is(2))
                .body("elenco[0].nomeAtor", is("Ator Principal"))
                .body("elenco[0].personagem", is("Herói"));
    }

    @Test
    void recusaImportarFilmeSemDataDeLancamento() {
        long tmdbId = tmdbIdUnico();
        when(clienteTMDB.buscarPorId(eq(tmdbId), isNull()))
                .thenReturn(filmeTmdb(tmdbId, "Sem data", null, null));

        given()
                .when().post("/filmes/importar-basico/{tmdbId}", tmdbId)
                .then()
                .statusCode(422);
    }

    @Test
    void reimportarOMesmoTmdbIdAtualizaEmVezDeDuplicar() {
        long tmdbId = tmdbIdUnico();
        int idOriginal = importarBasico(tmdbId, "Título Antigo");

        when(clienteTMDB.buscarPorId(eq(tmdbId), isNull()))
                .thenReturn(filmeTmdb(tmdbId, "Título Atualizado", LocalDate.of(2020, 1, 1), null));

        given()
                .when().post("/filmes/importar-basico/{tmdbId}", tmdbId)
                .then()
                .statusCode(200)
                .body("id", is(idOriginal))
                .body("titulo", is("Título Atualizado"));
    }

    @Test
    void buscaFilmePorIdRetorna200ComDadosCompletos() {
        long tmdbId = tmdbIdUnico();
        int id = importarBasico(tmdbId, "Filme Para Buscar");

        given()
                .when().get("/filmes/{id}", id)
                .then()
                .statusCode(200)
                .body("id", is(id))
                .body("titulo", is("Filme Para Buscar"));
    }

    @Test
    void respondeNaoEncontradoParaFilmeInexistente() {
        given()
                .when().get("/filmes/{id}", 999_999)
                .then()
                .statusCode(404)
                .body("mensagem", notNullValue());
    }

    @Test
    void listaFilmesFiltradosPorTitulo() {
        importarBasico(tmdbIdUnico(), "Interestelar");
        importarBasico(tmdbIdUnico(), "Interceptado");
        importarBasico(tmdbIdUnico(), "Outro Filme");

        given()
                .queryParam("titulo", "inter")
                .when().get("/filmes")
                .then()
                .statusCode(200)
                .body("itens.titulo", hasItem("Interestelar"))
                .body("itens.titulo", hasItem("Interceptado"));
    }

    @Test
    void recusaOrdenacaoPorCampoNaoPermitido() {
        given()
                .queryParam("ordenarPor", "id; drop table filme")
                .when().get("/filmes")
                .then()
                .statusCode(400);
    }

    @Test
    void recusaTamanhoDePaginaAcimaDoMaximo() {
        given()
                // Certifique-se de que RecursoDeFilme ou RecursoDeFilmes possui essa constante visível
                .queryParam("tamanho", RecursoDeFilme.TAMANHO_MAXIMO + 1)
                .when().get("/filmes")
                .then()
                .statusCode(400);
    }

    @Test
    void deletaFilmeComSucesso() {
        int id = importarBasico(tmdbIdUnico(), "Filme Para Deletar");

        given()
                .when().delete("/filmes/{id}", id)
                .then().statusCode(204);

        given()
                .when().get("/filmes/{id}", id)
                .then().statusCode(404);
    }

    @Test
    void respondeNaoEncontradoAoDeletarFilmeInexistente() {
        given()
                .when().delete("/filmes/{id}", 999_999)
                .then()
                .statusCode(404)
                .body("mensagem", notNullValue());
    }

    @Test
    void sincronizarPopularesAtualizaPopularidadeSemCriarFilmeNovo() {
        long tmdbId = tmdbIdUnico();
        importarBasico(tmdbId, "Filme Já Existente");

        when(clienteTMDB.listarPopulares(anyInt())).thenReturn(new ListarFilmesTMDBResponse(
                1,
                List.of(new ResumoFilmeTMDBResponse(tmdbId, "Filme Já Existente", 999.0),
                        new ResumoFilmeTMDBResponse(tmdbIdUnico(), "Filme Nunca Importado", 500.0)),
                1,
                2
        ));

        given()
                .queryParam("pagina", 1)
                .when().post("/filmes/sincronizar-populares")
                .then()
                .statusCode(200)
                .body("pagina", is(1))
                .body("atualizados", is(1))
                .body("ignorados", is(1));
    }
}