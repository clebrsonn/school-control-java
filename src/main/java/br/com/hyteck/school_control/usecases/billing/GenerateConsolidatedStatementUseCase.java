package br.com.hyteck.school_control.usecases.billing; // Ou pacote apropriado

import br.com.hyteck.school_control.web.dtos.billing.ConsolidatedStatement;
import br.com.hyteck.school_control.web.dtos.billing.StatementLineItem;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
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
public class GenerateConsolidatedStatementUseCase {
    /**
     * Repositório para acesso às faturas.
     */
    private final InvoiceRepository invoiceRepository;

    /**
     * Repositório para acesso aos responsáveis.
     */
    private final ResponsibleRepository responsibleRepository; // Injete o repositório

    /**
     * Construtor da classe GenerateConsolidatedStatementUseCase.
     *
     * @param invoiceRepository     Repositório para acesso às faturas.
     * @param responsibleRepository Repositório para acesso aos responsáveis.
     */
    public GenerateConsolidatedStatementUseCase(InvoiceRepository invoiceRepository,
                                                ResponsibleRepository responsibleRepository) {
        this.invoiceRepository = invoiceRepository;
        this.responsibleRepository = responsibleRepository;
    }

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

        // 3. Mapear Invoices para DTOs de Linha de Item
        List<StatementLineItem> items = individualInvoices.stream()
                .map(invoice -> new StatementLineItem(invoice.getId(),
                        invoice.getItems().getFirst().getEnrollment().getStudent().getName() // Assumindo que os dados estão carregados (cuidado com LAZY loading)
                        , (invoice.getDescription() + " - " + invoice.getItems().getFirst().getEnrollment().getClassroom().getName() + " - " + referenceMonth.toString()) // Exemplo
                        , (invoice.getAmount())
                        , (invoice.getDueDate())
                ))
                .collect(Collectors.toList());

        // 4. Calcular Totais e Datas
        BigDecimal totalAmountDue = items.stream()
                .map(StatementLineItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

        // 1. Buscar o Responsável (para obter o nome)

        // 2. Buscar todas as Invoices PENDENTES ou VENCIDAS para o responsável e mês de referência
        List<Invoice> individualInvoices = invoiceRepository.findPendingInvoicesByMonth(
                referenceMonth,
                List.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE) // Status que podem ser pagos
        );

        return individualInvoices.stream().map(invoice -> {
            List<StatementLineItem> items = invoice.getItems().stream()
                    .map(item -> new StatementLineItem(invoice.getId(),
                            item.getEnrollment().getStudent().getName()
                            , item.getDescription() // Exemplo
                            , (invoice.getAmount())
                            , (invoice.getDueDate())
                    ))
                    .collect(Collectors.toList());

            return new ConsolidatedStatement(
                    invoice.getResponsible().getId(),
                    invoice.getResponsible().getName() // Nome do responsável
                    , referenceMonth
                    , invoice.calculateAmount()
                    , invoice.getDueDate()
                    , items
                    // TODO: Gerar link de pagamento ou código de barras aqui, se aplicável
                    , ""
                    , ""
            );
        }).toList();
    }
}
