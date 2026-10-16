package br.ufrn.bomfilme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import br.ufrn.bomfilme.adaptadores.tmdb.ClienteTMDB;
import br.ufrn.bomfilme.adaptadores.tmdb.CreditosTMDBResponse;
import br.ufrn.bomfilme.adaptadores.tmdb.EquipeResponse;
import br.ufrn.bomfilme.adaptadores.tmdb.FilmeTMDBResponse;
import br.ufrn.bomfilme.adaptadores.tmdb.GeneroTMDBResponse;
import br.ufrn.bomfilme.adaptadores.tmdb.MembroElencoResponse;
import br.ufrn.bomfilme.dominio.RepositorioDeGeneros;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

/**
 * Regressões apontadas no review do PR #26.
 *
 * <p>As três passavam pela suíte anterior porque ela não cobria a interação entre as
 * duas formas de importação, nem o que sobra no banco quando a gravação falha no meio.
 */
@QuarkusTest
class ImportacaoDeFilmesTest {

    private static final AtomicLong PROXIMO_TMDB_ID = new AtomicLong(950_000);

    @InjectMock
    @RestClient
    ClienteTMDB clienteTMDB;

    @Inject
    RepositorioDeGeneros generos;

    private static long tmdbIdUnico() {
        return PROXIMO_TMDB_ID.incrementAndGet();
    }

    private static FilmeTMDBResponse filme(long tmdbId, String titulo, String sinopse,
                                           List<GeneroTMDBResponse> genres, CreditosTMDBResponse creditos) {
        return new FilmeTMDBResponse(tmdbId, titulo, titulo, sinopse, 120, "/poster.jpg",
                LocalDate.of(2020, 1, 1), 7.5, genres, creditos, 42.0);
    }

    private static CreditosTMDBResponse creditos() {
        return new CreditosTMDBResponse(
                List.of(new MembroElencoResponse(71L, "Ator Principal", "Herói", 0)),
                List.of(new EquipeResponse(79L, "Diretor Exemplo", "Director")));
    }

    /**
     * A importação básica não pede créditos, e não pedir não é o mesmo que receber uma
     * lista vazia. Antes da correção, importar o básico de um filme já completo zerava
     * a direção e o elenco dele.
     */
    @Test
    void importarOBasicoDepoisDoCompletoPreservaElencoEDirecao() {
        long tmdbId = tmdbIdUnico();
        when(clienteTMDB.buscarPorId(eq(tmdbId), eq("credits")))
                .thenReturn(filme(tmdbId, "Com Créditos", "Sinopse", List.of(new GeneroTMDBResponse(28L, "Ação")), creditos()));
        when(clienteTMDB.buscarPorId(eq(tmdbId), isNull()))
                .thenReturn(filme(tmdbId, "Com Créditos", "Sinopse", List.of(new GeneroTMDBResponse(28L, "Ação")), null));

        int id = given()
                .when().post("/filmes/importar-completo/{t}", tmdbId)
                .then().statusCode(200)
                .body("elenco.size()", is(1))
                .body("diretores", hasItem("Diretor Exemplo"))
                .extract().path("id");

        given()
                .when().post("/filmes/importar-basico/{t}", tmdbId)
                .then()
                .statusCode(200)
                .body("id", is(id))
                .body("elenco.size()", is(1))
                .body("elenco[0].nomeAtor", is("Ator Principal"))
                .body("diretores", hasItem("Diretor Exemplo"));
    }

    /** Pedir os créditos e o TMDB não trazer nada é outra coisa: aí sobrescreve. */
    @Test
    void importarOCompletoSemCreditosNoTmdbEsvaziaElencoEDirecao() {
        long tmdbId = tmdbIdUnico();
        when(clienteTMDB.buscarPorId(eq(tmdbId), eq("credits")))
                .thenReturn(filme(tmdbId, "Vai Esvaziar", "Sinopse", List.of(), creditos()));

        given().when().post("/filmes/importar-completo/{t}", tmdbId).then().statusCode(200);

        when(clienteTMDB.buscarPorId(eq(tmdbId), eq("credits")))
                .thenReturn(filme(tmdbId, "Vai Esvaziar", "Sinopse", List.of(),
                        new CreditosTMDBResponse(List.of(), List.of())));

        given()
                .when().post("/filmes/importar-completo/{t}", tmdbId)
                .then()
                .statusCode(200)
                .body("elenco.size()", is(0))
                .body("diretores.size()", is(0));
    }

    /**
     * A importação é uma transação só.
     *
     * <p>Resolver um gênero cria registro. Com a gravação do filme falhando depois —
     * aqui, sinopse nula contra uma coluna NOT NULL —, o gênero novo não pode sobrar.
     */
    @Test
    void falhaAoGravarOFilmeNaoDeixaGeneroNovoGravado() {
        long tmdbId = tmdbIdUnico();
        long generoInedito = 999_101L;
        when(clienteTMDB.buscarPorId(eq(tmdbId), isNull())).thenReturn(
                filme(tmdbId, "Vai Falhar", null,
                        List.of(new GeneroTMDBResponse(generoInedito, "Gênero Inédito")), null));

        given()
                .when().post("/filmes/importar-basico/{t}", tmdbId)
                .then().statusCode(500);

        boolean gravado = generos.listarTodos().stream()
                .anyMatch(genero -> generoInedito == genero.tmdbId());
        assertEquals(false, gravado,
                "o gênero foi confirmado numa transação própria: a importação não é atômica");
    }

    /**
     * Uma falha de persistência que não sabemos classificar ainda responde no formato
     * padrão de erro. Antes da correção, a exceção crua era repropagada e a resposta
     * caía no tratamento genérico do Quarkus, em `application/json`.
     */
    @Test
    void falhaDePersistenciaNaoClassificadaSaiEmProblemDetails() {
        long tmdbId = tmdbIdUnico();
        when(clienteTMDB.buscarPorId(eq(tmdbId), isNull()))
                .thenReturn(filme(tmdbId, "Sem Sinopse", null, List.of(), null));

        given()
                .when().post("/filmes/importar-basico/{t}", tmdbId)
                .then()
                .statusCode(500)
                .contentType(containsString("application/problem+json"))
                .body("type", is("urn:bomfilme:erro:erro-interno"))
                .body("title", is("Erro interno"))
                .body("status", is(500))
                // a mensagem não expõe detalhe interno; a causa fica no log
                .body("detail", is("erro interno ao acessar o banco de dados"));
    }
}
