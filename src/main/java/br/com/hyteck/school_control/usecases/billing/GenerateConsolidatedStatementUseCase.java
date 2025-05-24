package br.com.hyteck.school_control.usecases.billing; // Ou pacote apropriado

import br.com.hyteck.school_control.web.dtos.billing.ConsolidatedStatement;
import br.com.hyteck.school_control.web.dtos.billing.StatementLineItem;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.finance.Account;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.LedgerEntryRepository;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import br.com.hyteck.school_control.services.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Use case para gerar um extrato consolidado de faturas para um responsável em um mês de referência específico.
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class GenerateConsolidatedStatementUseCase {
    private final InvoiceRepository invoiceRepository;
    private final ResponsibleRepository responsibleRepository;
    private final AccountService accountService;
    private final LedgerEntryRepository ledgerEntryRepository;

    /**
     * Gera um extrato consolidado de faturas para um responsável em um mês de referência específico.
     *
     * @param responsibleId  ID do responsável.
     * @param referenceMonth Mês de referência para o extrato.
     * @return Um Optional contendo o extrato consolidado, ou Optional.empty() se não houver faturas pendentes.
     */
    @Transactional(readOnly = true) // Boa prática para operações de leitura
    public Optional<ConsolidatedStatement> execute(String responsibleId, YearMonth referenceMonth) {

        // 1. Buscar o Responsável (para obter o nome)
        Optional<Responsible> responsibleOpt = responsibleRepository.findById(responsibleId);
        if (responsibleOpt.isEmpty()) {
            // Lançar exceção ou retornar Optional.empty() se o responsável não existe
            return Optional.empty();
        }
        Responsible responsible = responsibleOpt.get();

        // 2. Buscar todas as Invoices PENDENTES ou VENCIDAS para o responsável e mês de referência
        List<Invoice> individualInvoices = invoiceRepository.findPendingInvoicesByResponsibleAndMonth(
                responsibleId,
                referenceMonth,
                List.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE) // Status que podem ser pagos
        );

        if (individualInvoices.isEmpty()) {
            return Optional.empty(); // Nenhuma fatura pendente para este período
        }

        // 3. Mapear Invoices para DTOs de Linha de Item, usando o saldo do ledger
        Account responsibleARAccount = accountService.findOrCreateResponsibleARAccount(responsibleId);
        log.info("Using A/R Account ID {} for Responsible ID {}", responsibleARAccount.getId(), responsibleId);

        List<StatementLineItem> items = individualInvoices.stream()
                .map(invoice -> {
                    BigDecimal currentBalanceDue = ledgerEntryRepository.getBalanceForInvoiceOnAccount(responsibleARAccount.getId(), invoice.getId());
                    log.debug("Ledger balance for Invoice ID {} on A/R Account ID {} is: {}", invoice.getId(), responsibleARAccount.getId(), currentBalanceDue);
                    return new StatementLineItem(
                            invoice.getId(),
                            // Assumindo que os dados estão carregados (cuidado com LAZY loading)
                            // Tentativa de obter nome do aluno do primeiro item, se existir.
                            invoice.getItems().isEmpty() ? "N/A" : invoice.getItems().getFirst().getEnrollment().getStudent().getName(),
                            (invoice.getDescription() + " - " +
                                    (invoice.getItems().isEmpty() ? "N/A" : invoice.getItems().getFirst().getEnrollment().getClassroom().getName()) +
                                    " - " + referenceMonth.toString()), // Exemplo
                            currentBalanceDue, // Usar o saldo do ledger
                            invoice.getDueDate()
                    );
                })
                .collect(Collectors.toList());

        // 4. Calcular Totais e Datas
        BigDecimal totalAmountDue = items.stream()
                .map(StatementLineItem::amount) // amount já é o currentBalanceDue
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.info("Total amount due for Responsible ID {} for month {}: {}", responsibleId, referenceMonth, totalAmountDue);

        LocalDate overallDueDate = individualInvoices.stream()
                .map(Invoice::getDueDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now()); // Data de vencimento geral (ex: a mais próxima)

        // 5. Montar o DTO Consolidado
        ConsolidatedStatement statement = new ConsolidatedStatement(
                responsibleId,
                responsible.getName() // Nome do responsável
                , referenceMonth
                , totalAmountDue
                , overallDueDate
                , items
                // TODO: Gerar link de pagamento ou código de barras aqui, se aplicável
                , ""
                , ""
        );

        return Optional.of(statement);
    }

    @Transactional(readOnly = true) // Boa prática para operações de leitura
    public List<ConsolidatedStatement> execute(YearMonth referenceMonth) {

        // 1. Buscar todas as Invoices PENDENTES ou VENCIDAS para o mês de referência
        List<Invoice> allInvoicesForMonth = invoiceRepository.findPendingInvoicesByMonth(
                referenceMonth,
                List.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE) // Status que podem ser pagos
        );

        if (allInvoicesForMonth.isEmpty()) {
            log.info("No PENDING or OVERDUE invoices found for month {}. Returning empty list.", referenceMonth);
            return List.of();
        }

        // Agrupar faturas por responsável para criar um extrato para cada um
        return allInvoicesForMonth.stream()
                .filter(invoice -> invoice.getResponsible() != null && invoice.getResponsible().getId() != null)
                .collect(Collectors.groupingBy(Invoice::getResponsible))
                .entrySet().stream()
                .map(entry -> {
                    Responsible responsible = entry.getKey();
                    List<Invoice> responsibleInvoices = entry.getValue();
                    log.info("Processing invoices for Responsible ID {} for month {}", responsible.getId(), referenceMonth);

                    Account responsibleARAccount = accountService.findOrCreateResponsibleARAccount(responsible.getId());
                    log.info("Using A/R Account ID {} for Responsible ID {}", responsibleARAccount.getId(), responsible.getId());

                    List<StatementLineItem> lineItems = responsibleInvoices.stream()
                            .map(invoice -> {
                                BigDecimal currentBalanceDue = ledgerEntryRepository.getBalanceForInvoiceOnAccount(responsibleARAccount.getId(), invoice.getId());
                                log.debug("Ledger balance for Invoice ID {} (Responsible ID {}) on A/R Account ID {} is: {}",
                                        invoice.getId(), responsible.getId(), responsibleARAccount.getId(), currentBalanceDue);
                                return new StatementLineItem(
                                        invoice.getId(),
                                        invoice.getItems().isEmpty() ? "N/A" : invoice.getItems().getFirst().getEnrollment().getStudent().getName(),
                                        (invoice.getDescription() + " - " +
                                                (invoice.getItems().isEmpty() ? "N/A" : invoice.getItems().getFirst().getEnrollment().getClassroom().getName()) +
                                                " - " + referenceMonth.toString()),
                                        currentBalanceDue,
                                        invoice.getDueDate()
                                );
                            })
                            .collect(Collectors.toList());

                    BigDecimal totalAmountDue = lineItems.stream()
                            .map(StatementLineItem::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    log.info("Total amount due for Responsible ID {} for month {}: {}", responsible.getId(), referenceMonth, totalAmountDue);

                    LocalDate overallDueDate = responsibleInvoices.stream()
                            .map(Invoice::getDueDate)
                            .min(LocalDate::compareTo)
                            .orElse(LocalDate.now());

                    return new ConsolidatedStatement(
                            responsible.getId(),
                            responsible.getName(),
                            referenceMonth,
                            totalAmountDue,
                            overallDueDate,
                            lineItems,
                            "", // paymentLink - to be implemented if needed
                            ""  // barcode - to be implemented if needed
                    );
                })
                .collect(Collectors.toList());
    }
}
