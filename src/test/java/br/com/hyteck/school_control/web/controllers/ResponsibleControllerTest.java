package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.usecases.billing.FindPaymentsByResponsibleId;
import br.com.hyteck.school_control.usecases.responsible.*;
import br.com.hyteck.school_control.usecases.student.FindStudentsByResponsibleId;
import br.com.hyteck.school_control.web.dtos.payments.PaymentResponse;
import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleRequest;
import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleResponse;
import br.com.hyteck.school_control.web.dtos.student.StudentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ResponsibleController.class)
class ResponsibleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private CreateResponsible createResponsibleUseCase;
    @MockBean
    private FindResponsibleById findResponsibleByIdUseCase;
    @MockBean
    private FindResponsibles findAllResponsiblesUseCase;
    @MockBean
    private UpdateResponsible updateResponsibleUseCase;
    @MockBean
    private DeleteResponsible deleteResponsibleUseCase;
    @MockBean
    private FindStudentsByResponsibleId findStudentsByResponsibleIdUseCase;
    @MockBean
    private FindPaymentsByResponsibleId findPaymentsByResponsibleIdUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    private ResponsibleRequest responsibleRequest;
    private ResponsibleResponse responsibleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        responsibleRequest = new ResponsibleRequest(
                "Jane Doe",
                "jane.doe@example.com",
                "987654321",
                "12345678900" // CPF
        );

        responsibleResponse = new ResponsibleResponse(
                "respId456",
                "Jane Doe",
                "jane.doe@example.com",
                "987654321"
        );
    }

    @Test
    void createResponsible_ShouldReturnCreated_WhenRequestIsValid() throws Exception {
        when(createResponsibleUseCase.execute(any(ResponsibleRequest.class))).thenReturn(responsibleResponse);

        mockMvc.perform(post("/responsibles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(responsibleRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", "http://localhost/responsibles/" + responsibleResponse.id()))
                .andExpect(jsonPath("$.id").value(responsibleResponse.id()))
                .andExpect(jsonPath("$.name").value(responsibleResponse.name()));

        verify(createResponsibleUseCase).execute(any(ResponsibleRequest.class));
    }

    @Test
    void createBulkResponsible_ShouldCallCreateUseCaseForEachRequest() throws Exception {
        List<ResponsibleRequest> requestList = List.of(responsibleRequest, responsibleRequest);
        when(createResponsibleUseCase.execute(any(ResponsibleRequest.class))).thenReturn(responsibleResponse); // Assuming it returns something, even if void controller

        mockMvc.perform(post("/responsibles/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestList)))
                .andExpect(status().isOk()); // Controller method is void, typically returns 200 OK

        verify(createResponsibleUseCase, times(requestList.size())).execute(any(ResponsibleRequest.class));
    }


    @Test
    void getResponsibleById_ShouldReturnResponsibleResponse_WhenExists() throws Exception {
        when(findResponsibleByIdUseCase.execute(anyString())).thenReturn(Optional.of(responsibleResponse));

        mockMvc.perform(get("/responsibles/{id}", "respId456")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(responsibleResponse.id()));

        verify(findResponsibleByIdUseCase).execute("respId456");
    }

    @Test
    void getResponsibleById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(findResponsibleByIdUseCase.execute(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/responsibles/{id}", "nonExistentId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(findResponsibleByIdUseCase).execute("nonExistentId");
    }

    @Test
    void getAllResponsibles_ShouldReturnPageOfResponsibleResponses() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        List<ResponsibleResponse> list = Collections.singletonList(responsibleResponse);
        Page<ResponsibleResponse> page = new PageImpl<>(list, pageable, list.size());
        when(findAllResponsiblesUseCase.execute(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/responsibles")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(responsibleResponse.id()))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(findAllResponsiblesUseCase).execute(any(Pageable.class));
    }

    @Test
    void updateResponsible_ShouldReturnUpdatedResponsibleResponse_WhenRequestIsValid() throws Exception {
        ResponsibleResponse updatedResponse = new ResponsibleResponse("respId456", "Jane Doe Updated", "new.email@example.com", "111222333");
        when(updateResponsibleUseCase.execute(anyString(), any(ResponsibleRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/responsibles/{id}", "respId456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(responsibleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe Updated"))
                .andExpect(jsonPath("$.email").value("new.email@example.com"));

        verify(updateResponsibleUseCase).execute(anyString(), any(ResponsibleRequest.class));
    }

    @Test
    void deleteResponsible_ShouldReturnNoContent_WhenResponsibleExists() throws Exception {
        doNothing().when(deleteResponsibleUseCase).execute(anyString());

        mockMvc.perform(delete("/responsibles/{id}", "respId456"))
                .andExpect(status().isNoContent());

        verify(deleteResponsibleUseCase).execute("respId456");
    }

    @Test
    void getStudentsByResponsibleId_ShouldReturnPageOfStudentResponses() throws Exception {
        String responsibleId = "respId456";
        Pageable pageable = PageRequest.of(0, 10);
        StudentResponse studentResponse = new StudentResponse("studId1", "Student Name", "student@example.com", "cpf", responsibleId, "Jane Doe", "Class A", LocalDateTime.now(), LocalDateTime.now());
        List<StudentResponse> studentList = Collections.singletonList(studentResponse);
        Page<StudentResponse> studentPage = new PageImpl<>(studentList, pageable, studentList.size());

        when(findStudentsByResponsibleIdUseCase.execute(eq(responsibleId), any(Pageable.class))).thenReturn(studentPage);

        mockMvc.perform(get("/responsibles/{responsibleId}/students", responsibleId)
                        .param("page", "0").param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("studId1"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(findStudentsByResponsibleIdUseCase).execute(eq(responsibleId), any(Pageable.class));
    }

    @Test
    void getPaymentsByResponsible_ShouldReturnListOfPaymentResponses() throws Exception {
        String responsibleId = "respId456";
        // Assuming PaymentResponse has a constructor or builder
        PaymentResponse paymentResponse = PaymentResponse.builder().id("paymentId1").invoiceId("invoiceId1").build();
        List<PaymentResponse> paymentList = Collections.singletonList(paymentResponse);

        when(findPaymentsByResponsibleIdUseCase.execute(responsibleId)).thenReturn(paymentList);

        mockMvc.perform(get("/responsibles/{responsibleId}/payments", responsibleId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("paymentId1"));

        verify(findPaymentsByResponsibleIdUseCase).execute(responsibleId);
    }
}
