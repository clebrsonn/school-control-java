package br.com.hyteck.school_control.web.dtos.responsible;

import br.com.hyteck.school_control.models.payments.Responsible;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.validator.constraints.br.CPF;

/**
 * Data Transfer Object (DTO) for creating or updating a {@link Responsible} party.
 * This record encapsulates the information needed to define a responsible person,
 * such as their name, contact details, and document ID.
 * Input validations are included for relevant fields.
 *
 * @param name     The full name of the responsible party. Must not be blank and have at least 2 characters.
 * @param email    The email address of the responsible party. Must be a valid email format. Can be null if not provided.
 * @param phone    The phone number of the responsible party. Must not be blank.
 * @param document The CPF document number of the responsible party. Must be a valid CPF format. Can be null if not provided.
 */
public record ResponsibleRequest(
        @NotBlank(message = "Name cannot be blank.")
        @Size(min = 2, message = "Name must have at least 2 characters.")
        String name,

        @Email(message = "Email should be valid.")
        // Email can be optional, so @NotBlank is not used here.
        String email,

        @NotBlank(message = "Phone number cannot be blank.")
        // Consider adding a @Pattern if a specific phone format is required.
        String phone,

        @CPF(message = "Document (CPF) must be valid.")
        // Document (CPF) can be optional.
        // Size validation for CPF is implicitly handled by @CPF, but can be added if specific length is needed outside format.
        String document
) {

    /**
     * Static factory method to convert a {@link ResponsibleRequest} DTO to a {@link Responsible} entity.
     * This method facilitates the mapping from the data transfer object to the domain model.
     * Note: The {@code document} field from the DTO is not currently mapped to the entity in this method.
     *
     * @param dto The {@link ResponsibleRequest} DTO to convert.
     * @return A {@link Responsible} entity populated with data from the DTO.
     */
    public static Responsible to(ResponsibleRequest dto) {
        // Uses the builder pattern from the Responsible entity.
        return Responsible.builder()
                .name(dto.name()) // Set responsible's name.
                .email(dto.email()) // Set responsible's email.
                .phone(dto.phone()) // Set responsible's phone number.
                // The 'document' field is not mapped here.
                // If it needs to be persisted, it should be added to the builder call: .document(dto.document())
                // Also, ensure the Responsible entity has a 'document' field and appropriate builder method.
                .build(); // Build the Responsible object.
    }

}