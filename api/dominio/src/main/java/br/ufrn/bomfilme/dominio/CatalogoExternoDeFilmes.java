package br.ufrn.bomfilme.dominio;

import java.util.List;

/**
 * Porta de saída para o catálogo de onde os filmes são importados — hoje, o TMDB.
 *
 * <p>O domínio enxerga só esta interface. O adaptador é que sabe de HTTP, token e do
 * formato de resposta do serviço externo, e é ele que traduz falhas de rede em
 * {@link ErroDeDominio.ServicoExternoIndisponivel}.
 */
public interface CatalogoExternoDeFilmes {

    /**
     * Busca um filme pelo id externo.
     *
     * @param comCreditos quando verdadeiro, traz também elenco e direção
     * @throws ErroDeDominio.NaoEncontrado quando o catálogo externo não conhece o id
     * @throws ErroDeDominio.ServicoExternoIndisponivel quando a consulta falha
     */
    FilmeDoCatalogo buscar(long idExterno, boolean comCreditos);

    /** Ranking de populares do catálogo externo, para atualizar a popularidade local. */
    List<PopularidadeExterna> populares(int pagina);
}
