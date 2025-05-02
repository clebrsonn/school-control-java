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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FindPaymentById {
    private static final Logger logger = LoggerFactory.getLogger(FindPaymentById.class);

    private final PaymentRepository paymentRepository;

    public FindPaymentById(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public Optional<PaymentResponse> execute(String id) {
        logger.info("Buscando pagamentos para o responsável ID (via Invoice): {}", id);

        return paymentRepository.findById(id).map(PaymentResponse::from);
    }

}
