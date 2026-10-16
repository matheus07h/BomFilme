package br.ufrn.bomfilme;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Corpo das respostas de erro no formato <em>problem details</em> da RFC 9457.
 *
 * <p>{@code type} identifica a classe do erro e é estável: o cliente pode comparar com
 * ela. {@code detail} descreve esta ocorrência e pode mudar de texto sem quebrar ninguém.
 * {@code violacoes} é um membro de extensão, permitido pela RFC, usado quando a falha
 * vem da validação de entrada e há mais de um campo a reportar.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Problema(String type, String title, int status, String detail, List<String> violacoes) {

    public static final String MEDIA_TYPE = "application/problem+json";

    public Problema(String type, String title, int status, String detail) {
        this(type, title, status, detail, null);
    }
}
