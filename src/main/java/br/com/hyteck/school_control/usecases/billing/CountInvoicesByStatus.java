package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class CountInvoicesByStatus {
    private final InvoiceRepository invoiceRepository;

    public CountInvoicesByStatus(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public Long execute(InvoiceStatus status){
        log.info("Contando faturas por status: {}", status.name());
        return invoiceRepository.countByStatus(status);
    }
}
