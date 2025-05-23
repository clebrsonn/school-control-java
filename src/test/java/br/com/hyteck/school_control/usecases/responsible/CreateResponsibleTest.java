package br.com.hyteck.school_control.usecases.responsible;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import br.com.hyteck.school_control.usecases.user.CreateVerificationToken;
import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleRequest;
import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CreateResponsibleTest {

    private ResponsibleRepository responsibleRepository;
    private CreateVerificationToken createVerificationToken;
    private CreateResponsible createResponsible;

    @BeforeEach
    void setUp() {
        responsibleRepository = mock(ResponsibleRepository.class);
        createVerificationToken = mock(CreateVerificationToken.class);
        createResponsible = new CreateResponsible(responsibleRepository, createVerificationToken);
    }

    @Test
    void execute_shouldCreateResponsible_whenDataIsValid() {
        ResponsibleRequest request = new ResponsibleRequest("John Doe", "john@email.com", "12345678901", null);
        when(responsibleRepository.save(any(Responsible.class))).thenAnswer(inv -> {
            Responsible r = inv.getArgument(0);
            r.setId("resp1");
            return r;
        });
        ResponsibleResponse response = createResponsible.execute(request);
        assertEquals("John Doe", response.name());
        assertEquals("resp1", response.id());
        verify(createVerificationToken).execute(any(Responsible.class));
    }

    @Test
    void execute_shouldGenerateEmailAndDocument_whenNotProvided() {
        ResponsibleRequest request = new ResponsibleRequest("Jane Doe", null, null, null);
        when(responsibleRepository.save(any(Responsible.class))).thenAnswer(inv -> {
            Responsible r = inv.getArgument(0);
            r.setId("resp2");
            return r;
        });
        ResponsibleResponse response = createResponsible.execute(request);
        assertNotNull(response.email());
        assertNotNull(response.id());
        verify(createVerificationToken).execute(any(Responsible.class));
    }
}
