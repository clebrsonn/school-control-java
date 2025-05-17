package br.com.hyteck.school_control.usecases.responsible;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import br.com.hyteck.school_control.usecases.user.CreateVerificationToken;
import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleRequest;
import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Log4j2
@RequiredArgsConstructor
public class CreateResponsible {

    private final ResponsibleRepository responsibleRepository;
    private final CreateVerificationToken createVerification;


    /**
     * Executa a lógica de negócio para criar um novo Responsável.
     * Valida os dados de entrada e verifica duplicidade de email e documento.
     *
     * @param requestDTO O DTO contendo os dados do responsável a ser criado.
     * @return O DTO representando o responsável criado.
     * @throws DuplicateResourceException Se o email ou documento já existirem.
     */
    @Transactional
    public ResponsibleResponse execute(@Valid ResponsibleRequest requestDTO) {
        log.info("Iniciando criação de responsável para email: {}", requestDTO.email());

        Responsible responsibleToSave = ResponsibleRequest.to(requestDTO);

        responsibleToSave.setUsername(RandomStringUtils.insecure().next(10));
        responsibleToSave.setPassword(RandomStringUtils.secure().nextAlphanumeric(10));

        responsibleToSave.setCredentialsNonExpired(true);
        Responsible savedResponsible = responsibleRepository.save(responsibleToSave);

        createVerification.execute(savedResponsible);

        log.info("Responsável criado com sucesso. ID: {}", savedResponsible.getId());

        return ResponsibleResponse.from(savedResponsible);
    }
}