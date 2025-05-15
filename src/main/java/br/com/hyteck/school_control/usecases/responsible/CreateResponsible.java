package br.com.hyteck.school_control.usecases.responsible;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.models.auth.VerificationToken;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import br.com.hyteck.school_control.repositories.VerificationTokenRepository;
import br.com.hyteck.school_control.usecases.notification.Notifications;
import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleRequest;
import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Log4j2
@RequiredArgsConstructor
public class CreateResponsible {

    private final ResponsibleRepository responsibleRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository tokenRepository;
    private final Notifications notifications; // Injetar EmailService

    /**
     * Executa a lógica de negócio para criar um novo Responsável.
     * Valida os dados de entrada e verifica duplicidade de email e documento.
     *
     * @param requestDTO O DTO contendo os dados do responsável a ser criado.
     * @return O DTO representando o responsável criado.
     * @throws DuplicateResourceException Se o email ou documento já existirem.
     */
    @Transactional // Garante que a operação seja atômica
    public ResponsibleResponse execute(@Valid ResponsibleRequest requestDTO) {
        log.info("Iniciando criação de responsável para email: {}", requestDTO.email());

        // 1. Verificar duplicidade (antes de tentar salvar)
        if (responsibleRepository.existsByEmail(requestDTO.email())) {
            log.warn("Tentativa de criar responsável com email duplicado: {}", requestDTO.email());
            // Você pode buscar a mensagem de um arquivo de propriedades também
            throw new DuplicateResourceException("Email já cadastrado: " + requestDTO.email());
        }

        Responsible responsibleToSave = ResponsibleRequest.to(requestDTO);

        responsibleToSave.setUsername(responsibleToSave.getEmail());
        responsibleToSave.setPassword(RandomStringUtils.secure().nextAlphanumeric(10));

        responsibleToSave.setCredentialsNonExpired(true);
        Responsible savedResponsible = responsibleRepository.save(responsibleToSave);

        VerificationToken verificationToken = new VerificationToken(savedResponsible);
        tokenRepository.save(verificationToken);

        log.info("Responsável criado com sucesso. ID: {}", savedResponsible.getId());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notifications.send(verificationToken);
            }
        });

        return ResponsibleResponse.from(savedResponsible);
    }
}