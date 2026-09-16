package br.ufrn.bomfilme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
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

        assertEquals(1, cinemas.porRede(rede.id).size());
        assertEquals(rede.id, cinemas.porId(cinema.id).orElseThrow().getRedeId());
    }
}
