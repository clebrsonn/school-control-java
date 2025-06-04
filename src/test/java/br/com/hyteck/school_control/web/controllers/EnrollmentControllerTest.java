package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.payments.InvoiceItem;
import br.com.hyteck.school_control.models.payments.Types;
import br.com.hyteck.school_control.services.InvoiceService;
import br.com.hyteck.school_control.usecases.enrollment.CreateEnrollment;
import br.com.hyteck.school_control.usecases.enrollment.FindEnrollmentsByStudentId;
import br.com.hyteck.school_control.web.dtos.invoice.InvoiceItemUpdateRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnrollmentController.class)
public class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateEnrollment createEnrollmentUseCase; // Existing mock from EnrollmentController

    @MockBean
    private FindEnrollmentsByStudentId findEnrollmentsByStudentId; // Existing mock

    @MockBean
    private InvoiceService invoiceService; // New mock for the added functionality

    private final String enrollmentId = "enroll-xyz";
    private final String itemId = "item-123";

    @Test
    void shouldUpdateInvoiceItemAmountAndReturnOk() throws Exception {
        InvoiceItemUpdateRequestDto requestDto = new InvoiceItemUpdateRequestDto();
        BigDecimal newAmount = new BigDecimal("250.50");
        requestDto.setNewAmount(newAmount);

        InvoiceItem updatedInvoiceItem = InvoiceItem.builder()
                .id(itemId)
                .description("Test Item Description")
                .amount(newAmount)
                .type(Types.TUITION) // Example type
                .build();
        // Enrollment on InvoiceItem is not set, mapToInvoiceItemResponseDto handles null enrollment

        when(invoiceService.updateInvoiceItemAmount(eq(itemId), eq(newAmount))).thenReturn(updatedInvoiceItem);

        mockMvc.perform(patch("/enrollments/{enrollmentId}/invoice-items/{itemId}", enrollmentId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId))
                .andExpect(jsonPath("$.amount").value(newAmount.doubleValue()))
                .andExpect(jsonPath("$.description").value("Test Item Description"))
                .andExpect(jsonPath("$.type").value(Types.TUITION.toString()));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentInvoiceItem() throws Exception {
        InvoiceItemUpdateRequestDto requestDto = new InvoiceItemUpdateRequestDto();
        BigDecimal newAmount = new BigDecimal("100.00");
        requestDto.setNewAmount(newAmount);

        when(invoiceService.updateInvoiceItemAmount(eq(itemId), eq(newAmount)))
                .thenThrow(new ResourceNotFoundException("InvoiceItem not found with id: " + itemId));

        mockMvc.perform(patch("/enrollments/{enrollmentId}/invoice-items/{itemId}", enrollmentId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestWhenUpdateAmountIsNull() throws Exception {
        InvoiceItemUpdateRequestDto requestDto = new InvoiceItemUpdateRequestDto();
        requestDto.setNewAmount(null); // Invalid

        mockMvc.perform(patch("/enrollments/{enrollmentId}/invoice-items/{itemId}", enrollmentId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenUpdateAmountIsZero() throws Exception {
        InvoiceItemUpdateRequestDto requestDto = new InvoiceItemUpdateRequestDto();
        requestDto.setNewAmount(BigDecimal.ZERO); // Invalid

        mockMvc.perform(patch("/enrollments/{enrollmentId}/invoice-items/{itemId}", enrollmentId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenUpdateAmountIsNegative() throws Exception {
        InvoiceItemUpdateRequestDto requestDto = new InvoiceItemUpdateRequestDto();
        requestDto.setNewAmount(new BigDecimal("-50.00")); // Invalid

        mockMvc.perform(patch("/enrollments/{enrollmentId}/invoice-items/{itemId}", enrollmentId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }
}
