package br.com.hyteck.school_control.usecases.responsible;

import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
public class DeleteResponsible {

    private static final Logger logger = LoggerFactory.getLogger(DeleteResponsible.class);
    private final ResponsibleRepository responsibleRepository;
    // private final StudentRepository studentRepository; // Injete se for verificar dependências

    public DeleteResponsible(ResponsibleRepository responsibleRepository /*, StudentRepository studentRepository */) {
        this.responsibleRepository = responsibleRepository;
        // this.studentRepository = studentRepository;
    }

    @Transactional
    public void execute(String id) {
        logger.info("Iniciando exclusão do responsável com ID: {}", id);

        // 1. Verificar se o responsável existe antes de tentar deletar
        if (!responsibleRepository.existsById(id)) {
            logger.warn("Responsável não encontrado para exclusão. ID: {}", id);
            throw new ResourceNotFoundException("Responsável não encontrado com ID: " + id);
        }

        // 2. (Opcional mas MUITO recomendado) Verificar dependências
        // Ex: Não permitir excluir se houver estudantes associados
        /*
        if (studentRepository.existsByResponsibleId(id)) {
             logger.warn("Tentativa de excluir responsável ID {} que possui estudantes associados.", id);
             throw new BusinessRuleException("Não é possível excluir responsável com estudantes associados."); // Criar BusinessRuleException
        }
        */

        // 3. Deletar o responsável
        responsibleRepository.deleteById(id);
        logger.info("Responsável excluído com sucesso. ID: {}", id);
    }
}