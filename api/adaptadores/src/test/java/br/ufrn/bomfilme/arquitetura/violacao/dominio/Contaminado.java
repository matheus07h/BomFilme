package br.ufrn.bomfilme.arquitetura.violacao.dominio;

import jakarta.ws.rs.GET;

/**
 * Violação de propósito, só nos testes.
 *
 * <p>Esta classe mora num pacote chamado {@code dominio} e importa Jakarta REST, que é
 * exatamente o que a regra de arquitetura proíbe. Serve de controle: o teste
 * {@code aRegraDoDominioPegaUmaViolacao} confirma que a regra ainda acusa quando deve.
 * Sem esse controle, uma regra mal escrita passaria em silêncio e não protegeria nada.
 */
public class Contaminado {

    @GET
    public String naoDeveriaEstarNoDominio() {
        return "esta classe existe para a regra de arquitetura ter o que reprovar";
    }
}
