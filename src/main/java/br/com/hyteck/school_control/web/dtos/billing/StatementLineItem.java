package br.com.hyteck.school_control.web.dtos.billing;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Record representando uma linha do extrato consolidado (uma fatura individual).
 * É imutável por natureza.
 *
 * @param invoiceId     ID da fatura original (Invoice).
 * @param studentName   Nome do estudante associado.
 * @param description   Descrição da cobrança (ex: "Mensalidade Turma X - Agosto/2024").
 * @param amount        Valor da mensalidade individual.
 * @param dueDate       Data de vencimento individual.
 */
public record StatementLineItem(
        String invoiceId,
        String studentName,
        String description,
        BigDecimal amount,
        LocalDate dueDate
) { }