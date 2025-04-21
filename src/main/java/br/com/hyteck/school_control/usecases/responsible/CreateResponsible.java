package br.com.hyteck.school_control.usecases.responsible;

import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleRequest;
import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleResponse;
import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated // Habilita validação de parâmetros de método anotados com @Valid
public class CreateResponsible {

    private static final Logger logger = LoggerFactory.getLogger(CreateResponsible.class);

    private final ResponsibleRepository responsibleRepository;

    // Injeção de dependência via construtor
    public CreateResponsible(ResponsibleRepository responsibleRepository) {
        this.responsibleRepository = responsibleRepository;
    }

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
        logger.info("Iniciando criação de responsável para email: {}", requestDTO.email());

        // 1. Verificar duplicidade (antes de tentar salvar)
        if (responsibleRepository.existsByEmail(requestDTO.email())) {
            logger.warn("Tentativa de criar responsável com email duplicado: {}", requestDTO.email());
            // Você pode buscar a mensagem de um arquivo de propriedades também
            throw new DuplicateResourceException("Email já cadastrado: " + requestDTO.email());
        }

        // 2. Mapear DTO para Entidade
        Responsible responsibleToSave = ResponsibleRequest.to(requestDTO);

        // 3. Persistir a Entidade
        Responsible savedResponsible = responsibleRepository.save(responsibleToSave);
        logger.info("Responsável criado com sucesso. ID: {}", savedResponsible.getId());

        // 4. Mapear Entidade salva para DTO de Resposta
        return ResponsibleResponse.from(savedResponsible);
    }

    // Método auxiliar para mapeamento (pode ser movido para uma classe Mapper dedicada)
}