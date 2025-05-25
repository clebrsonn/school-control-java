package br.com.hyteck.school_control.web.dtos.responsible;

import br.com.hyteck.school_control.models.payments.Responsible;

/**
 * Data Transfer Object (DTO) for returning details of a {@link Responsible} party to clients.
 * This record provides a structured representation of a responsible person's information,
 * typically used after creation or when querying responsible party data.
 * It excludes sensitive information that should not be exposed, like a user password if linked.
 *
 * @param id    The unique identifier of the responsible party.
 * @param name  The full name of the responsible party.
 * @param email The email address of the responsible party.
 * @param phone The phone number of the responsible party.
 */
public record ResponsibleResponse(
        String id,
        String name,
        String email,
        String phone
) {
    /**
     * Static factory method to create a {@link ResponsibleResponse} from a {@link Responsible} entity.
     * This method handles the conversion from the domain model to the DTO.
     *
     * @param responsible The {@link Responsible} entity to convert.
     * @return A {@link ResponsibleResponse} populated with data from the Responsible entity,
     *         or {@code null} if the input responsible is {@code null}.
     */
    public static ResponsibleResponse from(Responsible responsible) {
        // Prevent NullPointerException if the input entity is null.
        if (responsible == null) {
            return null;
        }
        // Create and return a new ResponsibleResponse, mapping fields from the entity.
        return new ResponsibleResponse(
                responsible.getId(), // Map the responsible's unique ID.
                responsible.getName(), // Map the responsible's name.
                responsible.getEmail(), // Map the responsible's email.
                responsible.getPhone() // Map the responsible's phone number.
                // Note: The 'document' field, if present in the entity, is not included here.
                // If it should be part of the response, add it to the record and this mapping.
        );
    }
}