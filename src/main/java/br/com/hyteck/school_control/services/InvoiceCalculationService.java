package br.com.hyteck.school_control.services;

import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;

@Service
public class InvoiceCalculationService {

    private final InvoiceRepository invoiceRepository;

    @Autowired
    public InvoiceCalculationService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Calcula o valor total a ser recebido no mês, considerando invoices abertas (PENDING e OVERDUE).
     * @param referenceMonth mês de referência
     * @return soma dos valores das invoices abertas
     */
    public BigDecimal calcularTotalAReceberNoMes(YearMonth referenceMonth) {
        return invoiceRepository.sumAmountByReferenceMonthAndStatuses(
                referenceMonth,
                Arrays.asList(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE)
        );
    }
}

