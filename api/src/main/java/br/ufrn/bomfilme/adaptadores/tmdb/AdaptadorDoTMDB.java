package br.ufrn.bomfilme.adaptadores.tmdb;

import br.ufrn.bomfilme.dominio.CatalogoExternoDeFilmes;
import br.ufrn.bomfilme.dominio.ErroDeDominio;
import br.ufrn.bomfilme.dominio.FilmeDoCatalogo;
import br.ufrn.bomfilme.dominio.ParticipacaoDoCatalogo;
import br.ufrn.bomfilme.dominio.PopularidadeExterna;
import br.ufrn.bomfilme.dominio.ReferenciaExterna;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import java.util.function.Supplier;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Implementa a porta de catálogo externo em cima do TMDB.
 *
 * <p>É aqui que o formato do TMDB e as falhas de rede param: o caso de uso recebe
 * registros de domínio e {@link ErroDeDominio}, nunca DTOs do TMDB nem exceções de HTTP.
 */
@ApplicationScoped
class AdaptadorDoTMDB implements CatalogoExternoDeFilmes {

    /** Cargo que o TMDB usa para direção. */
    private static final String DIRECAO = "Director";

    @Inject
    @RestClient
    ClienteTMDB cliente;

    @Override
    public FilmeDoCatalogo buscar(long idExterno, boolean comCreditos) {
        FilmeTMDBResponse dados = chamar(() -> cliente.buscarPorId(idExterno, comCreditos ? "credits" : null));
        return new FilmeDoCatalogo(
                dados.id(),
                dados.title(),
                dados.originalTitle(),
                dados.overview(),
                dados.posterPath(),
                dados.runtime(),
                dados.releaseDate(),
                dados.voteAverage(),
                dados.popularity(),
                generos(dados),
                diretores(dados.credits()),
                elenco(dados.credits()));
    }

    @Override
    public List<PopularidadeExterna> populares(int pagina) {
        ListarFilmesTMDBResponse resposta = chamar(() -> cliente.listarPopulares(pagina));
        if (resposta.results() == null) {
            return List.of();
        }
        return resposta.results().stream()
                .map(item -> new PopularidadeExterna(item.id(), item.popularity()))
                .toList();
    }

    private static List<ReferenciaExterna> generos(FilmeTMDBResponse dados) {
        if (dados.genres() == null) {
            return List.of();
        }
        return dados.genres().stream()
                .map(genero -> new ReferenciaExterna(genero.tmdbId(), genero.name()))
                .toList();
    }

    private static List<ReferenciaExterna> diretores(CreditosTMDBResponse creditos) {
        if (creditos == null || creditos.crew() == null) {
            return List.of();
        }
        return creditos.crew().stream()
                .filter(membro -> DIRECAO.equals(membro.job()))
                .map(membro -> new ReferenciaExterna(membro.id(), membro.name()))
                .toList();
    }

    private static List<ParticipacaoDoCatalogo> elenco(CreditosTMDBResponse creditos) {
        if (creditos == null || creditos.cast() == null) {
            return List.of();
        }
        return creditos.cast().stream()
                .filter(membro -> membro.order() != null)
                .map(membro -> new ParticipacaoDoCatalogo(
                        membro.id(), membro.name(), membro.character(), membro.order()))
                .toList();
    }

    /** Traduz as falhas do TMDB para o vocabulário do domínio, sem vazar detalhe da chamada. */
    private static <T> T chamar(Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                throw new ErroDeDominio.NaoEncontrado("Não encontrado no TMDB");
            }
            throw new ErroDeDominio.ServicoExternoIndisponivel("Falha ao consultar o TMDB");
        } catch (ProcessingException e) {
            throw new ErroDeDominio.ServicoExternoIndisponivel("TMDB indisponível");
        }
    }
}
