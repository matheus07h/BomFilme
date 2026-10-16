package br.ufrn.bomfilme;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Comparator;
import java.util.List;

/**
 * Traduz as violações do Bean Validation em 400 no formato <em>problem details</em>.
 *
 * <p>Substitui o mapeador padrão do Quarkus, que responde num formato próprio
 * ({@code parameterViolations}) e não em {@code application/problem+json}.
 */
@Provider
public class MapeadorDeValidacao implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException excecao) {
        List<String> violacoes = excecao.getConstraintViolations().stream()
                .map(MapeadorDeValidacao::descrever)
                .sorted(Comparator.naturalOrder())
                .toList();
        return Problemas.entradaInvalida("a requisição tem campos inválidos", violacoes);
    }

    /** Usa só o último nó do caminho: o cliente conhece {@code tamanho}, não {@code listar.tamanho}. */
    private static String descrever(ConstraintViolation<?> violacao) {
        String campo = "";
        for (Path.Node no : violacao.getPropertyPath()) {
            campo = no.getName() == null ? campo : no.getName();
        }
        return campo.isEmpty() ? violacao.getMessage() : campo + ": " + violacao.getMessage();
    }
}
