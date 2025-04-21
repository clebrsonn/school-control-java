package br.com.hyteck.school_control.web.dtos.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
// Importante: Para garantir imutabilidade da lista, considere usar List.copyOf()
// ao criar a instância ou no construtor compacto.

/**
 * Record representando o extrato/boleto consolidado para um responsável.
 * É imutável por natureza.
 *
 * @param responsibleId    ID do responsável.
 * @param responsibleName  Nome do responsável.
 * @param referenceMonth   Mês/Ano de referência do extrato.
 * @param totalAmountDue   Soma dos valores das faturas individuais incluídas.
 * @param overallDueDate   Data de vencimento geral para o pagamento consolidado.
 * @param items            Lista dos detalhes das faturas individuais (StatementLineItemDTO).
 * @param paymentLink      Opcional: Link para gateway de pagamento.
 * @param barcode          Opcional: Código de barras para boleto único.
 */
public record ConsolidatedStatement(
        String responsibleId,
        String responsibleName,
        YearMonth referenceMonth,
        BigDecimal totalAmountDue,
        LocalDate overallDueDate,
        List<StatementLineItem> items, // A lista em si pode ser mutável! Veja nota.
        String paymentLink,
        String barcode
        // Adicionar outros campos conforme necessário
) {
    // O compilador gera automaticamente construtor, accessors, equals/hashCode/toString.

    // Nota sobre a lista 'items':
    // O record garante que a *referência* à lista 'items' não pode ser alterada
    // após a criação do objeto ConsolidatedStatementDTO. No entanto, se a lista
    // passada para o construtor for mutável, seu conteúdo *interno* ainda
    // poderia ser modificado externamente.
    // Para imutabilidade real, você pode usar um construtor compacto:
    /*
    public ConsolidatedStatementDTO { // Construtor compacto
        // Cria uma cópia imutável da lista recebida
        this.items = List.copyOf(items);
    }
    */
    // Ou garantir que a lista passada durante a criação já seja imutável.
}