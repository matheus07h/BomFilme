package br.ufrn.bomfilme;

import br.ufrn.bomfilme.dominio.Assento;
import br.ufrn.bomfilme.dominio.Cinema;
import br.ufrn.bomfilme.dominio.Filme;
import br.ufrn.bomfilme.dominio.RedeCinema;
import br.ufrn.bomfilme.dominio.RepositorioDeAssentos;
import br.ufrn.bomfilme.dominio.RepositorioDeCinemas;
import br.ufrn.bomfilme.dominio.RepositorioDeFilmes;
import br.ufrn.bomfilme.dominio.RepositorioDeRedes;
import br.ufrn.bomfilme.dominio.RepositorioDeSalas;
import br.ufrn.bomfilme.dominio.RepositorioDeSessoes;
import br.ufrn.bomfilme.dominio.Sala;
import br.ufrn.bomfilme.dominio.Sessao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monta dados de teste pelas portas de domínio.
 *
 * <p>Os testes não tocam nas entidades JPA de propósito: elas são detalhe do adaptador de
 * banco e nem são visíveis daqui. Construir as fixturas pelas mesmas portas que a
 * aplicação usa também evita montar um estado que a aplicação não conseguiria produzir.
 */
@ApplicationScoped
public class Fixtures {

    private static final AtomicLong PROXIMO_TMDB_ID = new AtomicLong(1_000_000);

    @Inject
    RepositorioDeRedes redes;

    @Inject
    RepositorioDeCinemas cinemas;

    @Inject
    RepositorioDeSalas salas;

    @Inject
    RepositorioDeAssentos assentos;

    @Inject
    RepositorioDeSessoes sessoes;

    @Inject
    RepositorioDeFilmes filmes;

    public static String nomeUnico(String prefixo) {
        return prefixo + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    public static long proximoTmdbId() {
        return PROXIMO_TMDB_ID.incrementAndGet();
    }

    public RedeCinema rede() {
        return redes.criar(nomeUnico("Rede"));
    }

    public Cinema cinema() {
        return cinemas.criar(rede().id(), nomeUnico("Cinema"), "Natal", "RN");
    }

    public Cinema cinemaDaRede(long redeId) {
        return cinemas.criar(redeId, nomeUnico("Cinema"), "Natal", "RN");
    }

    public Sala sala() {
        return salas.criar(cinema().id(), nomeUnico("Sala"));
    }

    public Sala salaDoCinema(long cinemaId) {
        return salas.criar(cinemaId, nomeUnico("Sala"));
    }

    public Assento assento(Sala sala, String fileira, int numero) {
        return assentos.criar(sala.id(), fileira, numero);
    }

    /** Filme com duração, que é o que a programação de sessões exige. */
    public Filme filme() {
        return filme(120);
    }

    public Filme filme(Integer duracaoMinutos) {
        String titulo = nomeUnico("Filme");
        return filmes.salvar(new Filme(
                null, proximoTmdbId(), titulo, titulo, "Sinopse de teste", "/poster.jpg",
                duracaoMinutos, LocalDate.of(2020, 1, 1), 7.0, null, 10.0,
                List.of(), List.of(), List.of()));
    }

    public Sessao sessao(Sala sala) {
        return sessao(sala, filme(), OffsetDateTime.now().plusDays(1));
    }

    public Sessao sessao(Sala sala, Filme filme, OffsetDateTime inicio) {
        return sessoes.criar(sala.id(), filme.id(), inicio,
                inicio.plusMinutes(filme.duracaoMinutos()));
    }

    public void ocupar(Sessao sessao, Assento assento) {
        assentos.ocupar(sessao.id(), assento.id());
    }
}
