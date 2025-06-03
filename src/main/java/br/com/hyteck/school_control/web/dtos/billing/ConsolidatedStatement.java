package br.com.hyteck.school_control.web.dtos.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
// Importante: Para garantir imutabilidade da lista, considere usar List.copyOf()
// ao criar a instância ou no construtor compacto.

/**
 * Represents a consolidated financial statement for a responsible party for a specific reference month.
 * This record is designed to be immutable. It aggregates multiple individual invoice items
 * into a single statement with a total amount due and an overall due date.
 *
 * Note on list immutability: While records provide shallow immutability, the {@code items} list,
 * if mutable, could be modified externally after the record's creation. To ensure deep
 * immutability, consider using {@code List.copyOf(items)} in a compact constructor or ensuring
 * that an immutable list is passed during instantiation.
 *
 * @param responsibleId    The unique identifier of the responsible party.
 * @param responsibleName  The name of the responsible party.
 * @param referenceMonth   The year and month to which this consolidated statement pertains.
 * @param totalAmountDue   The total sum of all individual invoice items included in this statement.
 * @param overallDueDate   The single due date for the payment of the total amount.
 * @param items            A list of {@link StatementLineItem} objects, detailing individual charges or invoices.
 * @param paymentLink      An optional URL or link that directs the user to a payment gateway for this statement.
 * @param barcode          An optional barcode string, typically for generating a printable bill (boleto in Brazil).
 */
public record ConsolidatedStatement(
        String responsibleId,
        String responsibleName,
        YearMonth referenceMonth,
        BigDecimal totalAmountDue,
        LocalDate overallDueDate,
        List<StatementLineItem> items,
        String paymentLink,
        String barcode
) {
    /**
     * Compact constructor for {@link ConsolidatedStatement}.
     * This constructor ensures that the provided list of items is stored as an immutable list
     * internally, protecting against external modifications after the record is created.
     *
     * @param responsibleId    The unique identifier of the responsible party.
     * @param responsibleName  The name of the responsible party.
     * @param referenceMonth   The year and month to which this consolidated statement pertains.
     * @param totalAmountDue   The total sum of all individual invoice items included in this statement.
     * @param overallDueDate   The single due date for the payment of the total amount.
     * @param items            A list of {@link StatementLineItem} objects, detailing individual charges or invoices.
     *                         This list will be copied into an immutable list.
     * @param paymentLink      An optional URL or link that directs the user to a payment gateway for this statement.
     * @param barcode          An optional barcode string, typically for generating a printable bill (boleto in Brazil).
     */
    public ConsolidatedStatement {
        items = (items == null) ? List.of() : List.copyOf(items);
    }
}