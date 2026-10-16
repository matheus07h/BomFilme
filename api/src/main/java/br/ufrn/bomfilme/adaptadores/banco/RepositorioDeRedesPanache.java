package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Busca;
import br.ufrn.bomfilme.dominio.RedeCinema;
import br.ufrn.bomfilme.dominio.RepositorioDeRedes;
import br.ufrn.bomfilme.dominio.Pagina;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
class RepositorioDeRedesPanache implements RepositorioDeRedes, PanacheRepository<RedeCinemaEntidade> {

    @Override
    public Pagina<RedeCinema> listar(String nome, int pagina, int tamanho) {
        PanacheQuery<RedeCinemaEntidade> consulta = Busca.vazia(nome)
                ? findAll(Sort.by("id"))
                : find("lower(nome) like ?1 escape '\\'", Sort.by("id"), Busca.contendo(nome));
        return Paginas.de(consulta, pagina, tamanho, RedeCinemaEntidade::paraDominio);
    }

    @Override
    public Optional<RedeCinema> porId(long id) {
        return findByIdOptional(id).map(RedeCinemaEntidade::paraDominio);
    }

    @Override
    @Transactional
    public RedeCinema criar(String nome) {
        RedeCinemaEntidade entidade = new RedeCinemaEntidade(nome);
        ViolacoesDoBanco.gravando(() -> persistAndFlush(entidade));
        return entidade.paraDominio();
    }

    @Override
    @Transactional
    public Optional<RedeCinema> atualizar(long id, String nome) {
        Optional<RedeCinemaEntidade> encontrada = findByIdOptional(id);
        encontrada.ifPresent(entidade -> entidade.nome = nome);
        ViolacoesDoBanco.gravando(this::flush);
        return encontrada.map(RedeCinemaEntidade::paraDominio);
    }

    @Override
    @Transactional
    public boolean remover(long id) {
        boolean removida = deleteById(id);
        ViolacoesDoBanco.gravando(this::flush);
        return removida;
    }
}
