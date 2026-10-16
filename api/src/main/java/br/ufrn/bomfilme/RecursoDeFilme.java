package br.ufrn.bomfilme;

import br.ufrn.bomfilme.dtos.response.FilmeResponse;
import br.ufrn.bomfilme.dtos.response.FilmeResumoResponse;
import br.ufrn.bomfilme.utils.FiltroDeFilmes;
import br.ufrn.bomfilme.utils.Pagina;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/filmes")
@Produces({MediaType.APPLICATION_JSON, Problema.MEDIA_TYPE})
@Tag(name = "Filmes")
public class RecursoDeFilme {

    @Inject
    ServicoDeFilme servico;

    @POST
    @Path("/importar-basico/{tmdbId}")
    @Operation(summary = "Importa um filme do TMDB sem elenco",
            description = """
                    Traz título, sinopse, duração, pôster e gêneros. Reimportar o mesmo `tmdbId` \
                    atualiza o filme já existente em vez de duplicar.""")
    @APIResponse(responseCode = "200", description = "Filme importado ou atualizado",
            content = @Content(schema = @Schema(implementation = FilmeResponse.class)))
    @APIResponse(responseCode = "404", description = "O TMDB não conhece esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "422", description = "O filme não tem data de lançamento no TMDB e não entra no catálogo",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "502", description = "O TMDB está indisponível ou recusou a consulta",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public FilmeResponse importarBasico(
            @Parameter(description = "Id do filme no TMDB") @PathParam("tmdbId") Long tmdbId) {
        return servico.importarBasico(tmdbId);
    }

    @POST
    @Path("/importar-completo/{tmdbId}")
    @Operation(summary = "Importa um filme do TMDB com elenco e diretores",
            description = "Igual ao básico, mais os seis primeiros nomes do elenco e os diretores.")
    @APIResponse(responseCode = "200", description = "Filme importado ou atualizado",
            content = @Content(schema = @Schema(implementation = FilmeResponse.class)))
    @APIResponse(responseCode = "404", description = "O TMDB não conhece esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "422", description = "O filme não tem data de lançamento no TMDB e não entra no catálogo",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "502", description = "O TMDB está indisponível ou recusou a consulta",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public FilmeResponse importarCompleto(
            @Parameter(description = "Id do filme no TMDB") @PathParam("tmdbId") Long tmdbId) {
        return servico.importarCompleto(tmdbId);
    }

    @GET
    @Operation(summary = "Lista os filmes do catálogo",
            description = "Listagem paginada com filtros e ordenação. Valores nulos vão sempre para o fim.")
    @APIResponse(responseCode = "200", description = "Página de filmes, vazia quando não há resultados",
            content = @Content(schema = @Schema(implementation = Pagina.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros de paginação, filtro ou ordenação inválidos",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Pagina<FilmeResumoResponse> listar(
            @Parameter(description = "Filtra pelo título ou pelo título original, sem diferenciar maiúsculas")
            @QueryParam("titulo")
            @Size(max = 100, message = "titulo deve ter no máximo 100 caracteres") String titulo,
            @Parameter(description = "Filtra pelos filmes de um gênero; use GET /generos para descobrir os ids")
            @QueryParam("generoId") Long generoId,
            @Parameter(description = "Filtra pelo ano de lançamento")
            @QueryParam("ano")
            @Min(value = 1888, message = "ano deve ser maior ou igual a 1888")
            @Max(value = 2100, message = "ano deve ser menor ou igual a 2100") Integer ano,
            @Parameter(description = "Campo de ordenação")
            @QueryParam("ordenarPor") @DefaultValue("titulo")
            @Pattern(regexp = "titulo|lancamento|nota|popularidade",
                    message = "ordenarPor deve ser titulo, lancamento, nota ou popularidade") String ordenarPor,
            @Parameter(description = "Direção da ordenação")
            @QueryParam("direcao") @DefaultValue("asc")
            @Pattern(regexp = "asc|desc", message = "direcao deve ser asc ou desc") String direcao,
            @Parameter(description = "Índice da página, começando em zero")
            @QueryParam("pagina") @DefaultValue("0")
            @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int pagina,
            @Parameter(description = "Quantidade de filmes por página, entre 1 e 100")
            @QueryParam("tamanho") @DefaultValue("20")
            @Min(value = 1, message = "tamanho deve ser maior ou igual a um")
            @Max(value = Paginacao.TAMANHO_MAXIMO, message = "tamanho deve ser menor ou igual a 100") int tamanho) {
        return servico.listar(new FiltroDeFilmes(titulo, generoId, ano, ordenarPor, direcao), pagina, tamanho);
    }

    @POST
    @Path("/sincronizar-populares")
    @Operation(summary = "Atualiza a popularidade dos filmes já importados",
            description = """
                    Não importa filme novo: compara o ranking de populares do TMDB com o catálogo e \
                    atualiza só a popularidade dos filmes que já estão aqui. Cada rede escolhe \
                    manualmente o que entra no catálogo.""")
    @APIResponse(responseCode = "200", description = "Quantos filmes foram atualizados e quantos foram ignorados",
            content = @Content(schema = @Schema(implementation = ResultadoSincronizacao.class)))
    @APIResponse(responseCode = "400", description = "Página fora da faixa aceita pelo TMDB",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "502", description = "O TMDB está indisponível ou recusou a consulta",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public ResultadoSincronizacao sincronizarPopulares(
            @Parameter(description = "Página do ranking de populares do TMDB, entre 1 e 500")
            @QueryParam("pagina") @DefaultValue("1")
            @Min(value = 1, message = "pagina deve ser maior ou igual a um")
            @Max(value = 500, message = "pagina deve ser menor ou igual a 500") int pagina) {
        return servico.sincronizarPopulares(pagina);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Busca um filme por id", description = "Traz gêneros, diretores e elenco.")
    @APIResponse(responseCode = "200", description = "O filme encontrado",
            content = @Content(schema = @Schema(implementation = FilmeResponse.class)))
    @APIResponse(responseCode = "404", description = "Não existe filme com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public FilmeResponse buscarPorId(
            @Parameter(description = "Id do filme no catálogo") @PathParam("id") long id) {
        return servico.buscarPorId(id);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Remove um filme do catálogo",
            description = "A remoção é recusada enquanto houver sessões programadas para o filme.")
    @APIResponse(responseCode = "204", description = "Filme removido")
    @APIResponse(responseCode = "404", description = "Não existe filme com esse id",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "O filme ainda possui sessões vinculadas",
            content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public void deletar(@Parameter(description = "Id do filme no catálogo") @PathParam("id") long id) {
        servico.deletar(id);
    }
}
