package br.ufrn.bomfilme;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

interface RepositorioDeSessoes {

    List<Sessao> listar(FiltroDeSessoes filtro, int pagina, int tamanho);

    Optional<Sessao> porId(long id);

    Sessao criar(Sala sala, long filmeId, OffsetDateTime inicio, OffsetDateTime fim);

    Optional<Sessao> atualizar(long id, Sala sala, long filmeId, OffsetDateTime inicio, OffsetDateTime fim);

    boolean remover(long id);

    /** Indica se outra sessão da sala ocupa parte do intervalo [inicio, fim), ignorando a sessão informada. */
    boolean existeConflito(long salaId, OffsetDateTime inicio, OffsetDateTime fim, Long ignorarSessaoId);

    boolean possuiOcupacoes(long sessaoId);
}

@ApplicationScoped
class RepositorioDeSessoesPanache implements RepositorioDeSessoes, PanacheRepository<Sessao> {

    @Override
    public List<Sessao> listar(FiltroDeSessoes filtro, int pagina, int tamanho) {
        List<String> condicoes = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();

        if (filtro.cinemaId() != null) {
            condicoes.add("sala.cinema.id = :cinemaId");
            parametros.put("cinemaId", filtro.cinemaId());
        }
        if (filtro.salaId() != null) {
            condicoes.add("sala.id = :salaId");
            parametros.put("salaId", filtro.salaId());
        }
        if (filtro.filmeId() != null) {
            condicoes.add("filmeId = :filmeId");
            parametros.put("filmeId", filtro.filmeId());
        }

        Sort ordem = Sort.by("inicio").and("id");
        PanacheQuery<Sessao> consulta = condicoes.isEmpty()
                ? findAll(ordem)
                : find(String.join(" and ", condicoes), ordem, parametros);
        return consulta.page(Page.of(pagina, tamanho)).list();
    }

    @Override
    public Optional<Sessao> porId(long id) {
        return findByIdOptional(id);
    }

    @Override
    @Transactional
    public Sessao criar(Sala sala, long filmeId, OffsetDateTime inicio, OffsetDateTime fim) {
        Sessao sessao = new Sessao(sala, filmeId, inicio, fim);
        persistAndFlush(sessao);
        return sessao;
    }

    @Override
    @Transactional
    public Optional<Sessao> atualizar(long id, Sala sala, long filmeId, OffsetDateTime inicio, OffsetDateTime fim) {
        Optional<Sessao> encontrada = findByIdOptional(id);
        encontrada.ifPresent(sessao -> {
            sessao.sala = sala;
            sessao.filmeId = filmeId;
            sessao.inicio = inicio;
            sessao.fim = fim;
        });
        flush();
        return encontrada;
    }

    @Override
    @Transactional
    public boolean remover(long id) {
        boolean removida = deleteById(id);
        flush();
        return removida;
    }

    @Override
    public boolean existeConflito(long salaId, OffsetDateTime inicio, OffsetDateTime fim, Long ignorarSessaoId) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("salaId", salaId);
        parametros.put("inicio", inicio);
        parametros.put("fim", fim);
        String consulta = "sala.id = :salaId and inicio < :fim and fim > :inicio";
        if (ignorarSessaoId != null) {
            consulta += " and id <> :ignorar";
            parametros.put("ignorar", ignorarSessaoId);
        }
        return count(consulta, parametros) > 0;
    }

    @Override
    public boolean possuiOcupacoes(long sessaoId) {
        return getEntityManager()
                .createQuery("select count(o) from Ocupacao o where o.sessao.id = :sessaoId", Long.class)
                .setParameter("sessaoId", sessaoId)
                .getSingleResult() > 0;
    }
}

interface RepositorioDeAssentos {

    List<Assento> porSala(long salaId);

    List<Assento> listarDaSala(long salaId, String fileira, int pagina, int tamanho);

    Optional<Assento> porId(long id);

    Assento criar(Sala sala, String fileira, int numero);

    Optional<Assento> atualizar(long id, String fileira, int numero);

    boolean remover(long id);

    boolean possuiOcupacoes(long assentoId);

    Set<Long> ocupadosNaSessao(long sessaoId);

    void ocupar(Sessao sessao, Assento assento);
}

@ApplicationScoped
class RepositorioDeAssentosPanache implements RepositorioDeAssentos, PanacheRepository<Assento> {

    @Override
    public List<Assento> porSala(long salaId) {
        return find("sala.id", Sort.by("fileira").and("numero"), salaId).list();
    }

    @Override
    public List<Assento> listarDaSala(long salaId, String fileira, int pagina, int tamanho) {
        Sort ordem = Sort.by("fileira").and("numero").and("id");
        PanacheQuery<Assento> consulta = Busca.vazia(fileira)
                ? find("sala.id = ?1", ordem, salaId)
                : find("sala.id = ?1 and fileira = ?2", ordem, salaId, fileira);
        return consulta.page(Page.of(pagina, tamanho)).list();
    }

    @Override
    public Optional<Assento> porId(long id) {
        return findByIdOptional(id);
    }

    @Override
    @Transactional
    public Assento criar(Sala sala, String fileira, int numero) {
        Assento assento = new Assento(sala, fileira, numero);
        persistAndFlush(assento);
        return assento;
    }

    @Override
    @Transactional
    public Optional<Assento> atualizar(long id, String fileira, int numero) {
        Optional<Assento> encontrado = findByIdOptional(id);
        encontrado.ifPresent(assento -> {
            assento.fileira = fileira;
            assento.numero = numero;
        });
        flush();
        return encontrado;
    }

    @Override
    @Transactional
    public boolean remover(long id) {
        boolean removido = deleteById(id);
        flush();
        return removido;
    }

    @Override
    public boolean possuiOcupacoes(long assentoId) {
        return getEntityManager()
                .createQuery("select count(o) from Ocupacao o where o.assento.id = :assentoId", Long.class)
                .setParameter("assentoId", assentoId)
                .getSingleResult() > 0;
    }

    @Override
    public Set<Long> ocupadosNaSessao(long sessaoId) {
        return getEntityManager()
                .createQuery("select o.assento.id from Ocupacao o where o.sessao.id = :sessaoId", Long.class)
                .setParameter("sessaoId", sessaoId)
                .getResultStream()
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void ocupar(Sessao sessao, Assento assento) {
        getEntityManager().persist(new Ocupacao(sessao, assento));
        getEntityManager().flush();
    }
}
