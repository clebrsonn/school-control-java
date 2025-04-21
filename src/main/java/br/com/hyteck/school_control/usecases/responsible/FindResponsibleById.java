package br.com.hyteck.school_control.usecases.responsible;

import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleResponse;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class FindResponsibleById {
    private static final Logger logger = LoggerFactory.getLogger(FindResponsibleById.class);
    private final ResponsibleRepository responsibleRepository;

    public FindResponsibleById(ResponsibleRepository responsibleRepository) {
        this.responsibleRepository = responsibleRepository;
    }

    @Transactional(readOnly = true) // Otimização para operações de leitura
    public Optional<ResponsibleResponse> execute(String id) {
        logger.debug("Buscando responsável com ID: {}", id);
        return responsibleRepository.findById(id)
                .map(ResponsibleResponse::from); // Mapeia para DTO se encontrado
    }
}
