package br.ufrn.bomfilme;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioDeRedesPanache implements RepositorioDeRedes, PanacheRepository<RedeCinema> {

    @Override
    public List<RedeCinema> listar(String nome, int pagina, int tamanho) {
        PanacheQuery<RedeCinema> consulta = Busca.vazia(nome)
                ? findAll(Sort.by("id"))
                : find("lower(nome) like ?1 escape '\\'", Sort.by("id"), Busca.contendo(nome));
        return consulta.page(Page.of(pagina, tamanho)).list();
    }

    @Override
    public Optional<RedeCinema> porId(long id) {
        return findByIdOptional(id);
    }

    @Override
    @Transactional
    public RedeCinema criar(String nome) {
        RedeCinema rede = new RedeCinema(nome);
        persistAndFlush(rede);
        return rede;
    }

    @Override
    @Transactional
    public Optional<RedeCinema> atualizar(long id, String nome) {
        Optional<RedeCinema> encontrada = findByIdOptional(id);
        encontrada.ifPresent(rede -> rede.nome = nome);
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
}
