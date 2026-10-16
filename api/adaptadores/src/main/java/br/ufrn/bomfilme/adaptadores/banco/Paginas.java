package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Pagina;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import java.util.function.Function;

/**
 * Monta uma {@link Pagina} a partir de uma consulta Panache.
 *
 * <p>A paginação vai para o SQL — {@code LIMIT}/{@code OFFSET} pelo {@code page(...)} —
 * e o total vem de um {@code count} separado, não do tamanho da lista carregada.
 */
final class Paginas {

    private Paginas() {
    }

    static <E, T> Pagina<T> de(PanacheQuery<E> consulta, int pagina, int tamanho, Function<E, T> conversor) {
        PanacheQuery<E> paginada = consulta.page(Page.of(pagina, tamanho));
        return Pagina.de(
                paginada.list().stream().map(conversor).toList(),
                pagina, tamanho, paginada.count());
    }
}
