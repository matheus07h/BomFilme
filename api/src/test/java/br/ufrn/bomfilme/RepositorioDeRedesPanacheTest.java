package br.ufrn.bomfilme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositorioDeRedesPanacheTest {

    @Inject
    RepositorioDeRedes redes;

    @Inject
    RepositorioDeCinemas cinemas;

    @Test
    void persisteUmaRedeELeDeVolta() {
        RedeCinema criada = redes.criar("Rede de teste");

        assertTrue(criada.id != null, "a identidade deve vir do banco");
        assertEquals("Rede de teste", redes.porId(criada.id).orElseThrow().nome);
    }

    @Test
    void persisteUmCinemaLigadoAUmaRede() {
        RedeCinema rede = redes.criar("Rede com cinema");
        Cinema cinema = cinemas.criar(rede, "Praia Shopping", "Natal", "RN");

        var filtro = new FiltroDeCinemas(rede.id, null, null, null);
        assertEquals(1, cinemas.listar(filtro, 0, Paginacao.TAMANHO_PADRAO).size());
        assertEquals(rede.id, cinemas.porId(cinema.id).orElseThrow().getRedeId());
    }

    @Test
    void atualizaERemoveUmaRede() {
        RedeCinema rede = redes.criar("Rede a atualizar");

        assertEquals("Rede atualizada", redes.atualizar(rede.id, "Rede atualizada").orElseThrow().nome);
        assertTrue(redes.atualizar(-1, "Inexistente").isEmpty());

        assertTrue(redes.remover(rede.id));
        assertTrue(redes.porId(rede.id).isEmpty());
        assertFalse(redes.remover(rede.id));
    }

    @Test
    void bancoRecusaRemoverRedeComCinemasEOMapeadorTraduzParaConflito() {
        RedeCinema rede = redes.criar("Rede protegida pelo banco");
        cinemas.criar(rede, "Cinema protegido", "Natal", "RN");

        PersistenceException excecao = assertThrows(PersistenceException.class, () -> redes.remover(rede.id));

        Response resposta = new MapeadorDeErrosDePersistencia().toResponse(excecao);
        assertEquals(409, resposta.getStatus());
        assertTrue(redes.porId(rede.id).isPresent());
    }

    @Test
    void atualizaERemoveUmCinema() {
        RedeCinema origem = redes.criar("Rede de origem");
        RedeCinema destino = redes.criar("Rede de destino");
        Cinema cinema = cinemas.criar(origem, "Cinema antigo", "Natal", "RN");

        Cinema atualizado = cinemas.atualizar(cinema.id, destino, "Cinema novo", "Recife", "PE").orElseThrow();

        assertEquals(destino.id, atualizado.getRedeId());
        assertEquals("Cinema novo", atualizado.nome);
        assertTrue(cinemas.existePorRede(destino.id));
        assertFalse(cinemas.existePorRede(origem.id));
        assertFalse(cinemas.possuiSalas(cinema.id));

        assertTrue(cinemas.remover(cinema.id));
        assertFalse(cinemas.remover(cinema.id));
    }
}
