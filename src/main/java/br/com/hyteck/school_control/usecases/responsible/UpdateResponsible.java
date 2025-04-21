package br.com.hyteck.school_control.usecases.responsible;

import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleRequest;
import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleResponse;
import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

@Service
@Validated
public class UpdateResponsible {

    private static final Logger logger = LoggerFactory.getLogger(UpdateResponsible.class);
    private final ResponsibleRepository responsibleRepository;

    public UpdateResponsible(ResponsibleRepository responsibleRepository) {
        this.responsibleRepository = responsibleRepository;
    }

    @Transactional
    public ResponsibleResponse execute(String id, @Valid ResponsibleRequest requestDTO) {
        logger.info("Iniciando atualização do responsável com ID: {}", id);

        // 1. Buscar o responsável existente
        Responsible existingResponsible = responsibleRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Responsável não encontrado para atualização. ID: {}", id);
                    return new ResourceNotFoundException("Responsável não encontrado com ID: " + id);
                });

        // 2. Verificar duplicidade SE email ou documento foram alterados
        checkDuplicates(requestDTO, existingResponsible);

        // 3. Atualizar os dados da entidade existente
        updateEntityFromDto(existingResponsible, requestDTO);

        // 4. Salvar a entidade atualizada
        Responsible updatedResponsible = responsibleRepository.save(existingResponsible);
        logger.info("Responsável atualizado com sucesso. ID: {}", updatedResponsible.getId());

        // 5. Retornar DTO de resposta
        return ResponsibleResponse.from(updatedResponsible);
    }

    private void checkDuplicates(ResponsibleRequest requestDTO, Responsible existingResponsible) {
        // Verifica email apenas se foi alterado
        if (!Objects.equals(requestDTO.email(), existingResponsible.getEmail())) {
            responsibleRepository.findByEmail(requestDTO.email()).ifPresent(duplicate -> {
                // Permite se o duplicado for o próprio registro sendo atualizado (caso raro, mas seguro)
                if (!Objects.equals(duplicate.getId(), existingResponsible.getId())) {
                    logger.warn("Tentativa de atualizar responsável ID {} com email duplicado: {}", existingResponsible.getId(), requestDTO.email());
                    throw new DuplicateResourceException("Email já cadastrado para outro responsável: " + requestDTO.email());
                }
            });
        }
    }

    private void updateEntityFromDto(Responsible entity, ResponsibleRequest dto) {
        BeanUtils.copyProperties(dto, entity, "id");
    }
}