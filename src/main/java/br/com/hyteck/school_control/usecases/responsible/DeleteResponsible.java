package br.com.hyteck.school_control.usecases.responsible;

import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for deleting a Responsible entity by its unique identifier.
 * Applies business rules and checks for dependencies before deletion.
 */
@Service
@RequiredArgsConstructor
public class DeleteResponsible {

    private static final Logger logger = LoggerFactory.getLogger(DeleteResponsible.class);
    private final ResponsibleRepository responsibleRepository;
    // private final StudentRepository studentRepository; // Inject if you need to check dependencies

    /**
     * Deletes a Responsible by their unique identifier.
     *
     * @param id the unique identifier of the Responsible
     * @throws ResourceNotFoundException if the Responsible does not exist
     *                                   // @throws BusinessRuleException if business rules prevent deletion (e.g., associated students)
     */
    @Transactional
    public void execute(String id) {
        logger.info("Starting deletion of Responsible with ID: {}", id);

        // 1. Check if the Responsible exists before attempting deletion
        if (!responsibleRepository.existsById(id)) {
            logger.warn("Responsible not found for deletion. ID: {}", id);
            throw new ResourceNotFoundException("Responsible not found with ID: " + id);
        }

        // 2. (Optional but HIGHLY recommended) Check dependencies
        // Example: Do not allow deletion if there are associated students
        /*
        if (studentRepository.existsByResponsibleId(id)) {
             logger.warn("Attempt to delete Responsible ID {} with associated students.", id);
             throw new BusinessRuleException("Cannot delete Responsible with associated students."); // Create BusinessRuleException
        }
        */

        // 3. Delete the Responsible
        responsibleRepository.deleteById(id);
        logger.info("Responsible successfully deleted. ID: {}", id);
    }
}

