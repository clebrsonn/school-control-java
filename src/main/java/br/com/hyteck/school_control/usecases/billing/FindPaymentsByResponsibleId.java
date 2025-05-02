package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.payments.Payment;
import br.com.hyteck.school_control.repositories.PaymentRepository;
import br.com.hyteck.school_control.web.dtos.payments.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class FindPaymentsByResponsibleId {
    private static final Logger logger = LoggerFactory.getLogger(FindPaymentsByResponsibleId.class);

    private final PaymentRepository paymentRepository;

    public FindPaymentsByResponsibleId(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> execute(String responsibleId) {
        logger.info("Buscando pagamentos para o responsável ID (via Invoice): {}", responsibleId);

        // Usa o método simplificado do repositório
        List<Payment> payments = paymentRepository.findByResponsibleId(responsibleId);

        if (payments.isEmpty()) {
            logger.info("Nenhum pagamento encontrado para o responsável ID: {}", responsibleId);
            return Collections.emptyList();
        }

        // Mapear para DTOs
        return payments.stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }
}
