package br.ufrn.bomfilme.adaptadores.banco;

import br.ufrn.bomfilme.dominio.Participacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Associação entre filme e pessoa que guarda o personagem e a ordem de crédito. */
@Entity
@Table(name = "elenco_filme")
class ElencoFilmeEntidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "filme_id", nullable = false)
    FilmeEntidade filme;

    @ManyToOne
    @JoinColumn(name = "pessoa_id", nullable = false)
    PessoaEntidade pessoa;

    @Column(nullable = false)
    String personagem;

    @Column(name = "ordem_bilheteria", nullable = false)
    Integer ordem;

    ElencoFilmeEntidade() {
    }

    ElencoFilmeEntidade(FilmeEntidade filme, PessoaEntidade pessoa, String personagem, Integer ordem) {
        this.filme = filme;
        this.pessoa = pessoa;
        this.personagem = personagem;
        this.ordem = ordem;
    }

    Participacao paraDominio() {
        return new Participacao(pessoa.paraDominio(), personagem, ordem);
    }
}
