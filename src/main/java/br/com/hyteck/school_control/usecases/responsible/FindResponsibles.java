package br.com.hyteck.school_control.usecases.responsible;

import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleResponse;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
public class FindResponsibles {
    private static final Logger logger = LoggerFactory.getLogger(FindResponsibles.class);
    private final ResponsibleRepository responsibleRepository;

    public FindResponsibles(ResponsibleRepository responsibleRepository) {
        this.responsibleRepository = responsibleRepository;
    }

    @Transactional(readOnly = true) // Otimização para operações de leitura
    public Page<ResponsibleResponse> execute(Pageable pageable) {
        logger.info("Buscando todos os responsáveis paginados: {}", pageable);
        return responsibleRepository.findAll(pageable)
                .map(ResponsibleResponse::from); // Mapeia para DTO se encontrado
    }
}
