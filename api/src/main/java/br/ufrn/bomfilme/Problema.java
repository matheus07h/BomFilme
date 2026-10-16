package br.ufrn.bomfilme;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Corpo das respostas de erro no formato <em>problem details</em> da RFC 9457.
 *
 * <p>{@code type} identifica a classe do erro e é estável: o cliente pode comparar com
 * ela. {@code detail} descreve esta ocorrência e pode mudar de texto sem quebrar ninguém.
 * {@code violacoes} é um membro de extensão, permitido pela RFC, usado quando a falha
 * vem da validação de entrada e há mais de um campo a reportar.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "Problema", description = "Erro no formato problem details (RFC 9457)")
public record Problema(

        @Schema(description = "URN estável que identifica a classe do erro",
                examples = "urn:bomfilme:erro:nao-encontrado")
        String type,

        @Schema(description = "Resumo legível da classe do erro", examples = "Recurso não encontrado")
        String title,

        @Schema(description = "Código de status HTTP da resposta", examples = "404")
        int status,

        @Schema(description = "Descrição desta ocorrência", examples = "cinema 42 não encontrado")
        String detail,

        @Schema(description = "Campos recusados pela validação; ausente nos outros erros",
                examples = "tamanho: tamanho deve ser menor ou igual a 100")
        List<String> violacoes) {

    public static final String MEDIA_TYPE = "application/problem+json";

    public Problema(String type, String title, int status, String detail) {
        this(type, title, status, detail, null);
    }
}
