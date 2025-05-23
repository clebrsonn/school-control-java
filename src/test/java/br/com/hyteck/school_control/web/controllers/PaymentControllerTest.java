package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.Payment;
import br.com.hyteck.school_control.models.payments.PaymentMethod;
import br.com.hyteck.school_control.models.payments.PaymentStatus;
import br.com.hyteck.school_control.usecases.billing.FindPaymentById;
import br.com.hyteck.school_control.usecases.billing.ProcessPaymentUseCase;
import br.com.hyteck.school_control.web.dtos.payments.PaymentRequest;
import br.com.hyteck.school_control.web.dtos.payments.PaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private ProcessPaymentUseCase processPaymentUseCase;

    @MockBean
    private FindPaymentById findPaymentByIdUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    private PaymentRequest paymentRequest;
    private Payment payment;
    private PaymentResponse paymentResponse;
    private Invoice mockInvoice;


    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockInvoice = Invoice.builder().id("invoiceId123").build();


        paymentRequest = new PaymentRequest(
                "invoiceId123",
                new BigDecimal("100.00"),
                PaymentMethod.CREDIT_CARD
        );

        payment = Payment.builder()
                .id("paymentId123")
                .invoice(mockInvoice) // Associate the mock invoice
                .amountPaid(new BigDecimal("100.00"))
                .paymentDate(LocalDateTime.now())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.COMPLETED)
                .build();

        paymentResponse = PaymentResponse.from(payment);
    }

    @Test
    void processPayment_ShouldReturnPaymentResponse_WhenRequestIsValid() throws Exception {
        // Arrange
        when(processPaymentUseCase.execute(anyString(), any(BigDecimal.class), any(PaymentMethod.class)))
                .thenReturn(payment);

        // Act & Assert
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getId()))
                .andExpect(jsonPath("$.amount").value(payment.getAmountPaid().doubleValue()))
                .andExpect(jsonPath("$.invoiceId").value(payment.getInvoice().getId()));


        verify(processPaymentUseCase).execute(
                paymentRequest.invoiceId(),
                paymentRequest.amount(),
                paymentRequest.paymentMethod()
        );
    }

    @Test
    void processPayment_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // Arrange
        PaymentRequest invalidRequest = new PaymentRequest(
                null, // Invalid: invoiceId is blank
                new BigDecimal("100.00"),
                PaymentMethod.CREDIT_CARD
        );

        // Act & Assert
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPaymentById_ShouldReturnPaymentResponse_WhenPaymentExists() throws Exception {
        // Arrange
        String paymentId = "paymentId123";
        when(findPaymentByIdUseCase.execute(paymentId)).thenReturn(Optional.of(paymentResponse));


        // Act & Assert
        mockMvc.perform(get("/payments/{id}", paymentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentResponse.getId()))
                .andExpect(jsonPath("$.amount").value(paymentResponse.getAmount().doubleValue()));

        verify(findPaymentByIdUseCase).execute(paymentId);
    }

    @Test
    void getPaymentById_ShouldReturnNotFound_WhenPaymentDoesNotExist() throws Exception {
        // Arrange
        String paymentId = "nonExistentId";
        when(findPaymentByIdUseCase.execute(paymentId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/payments/{id}", paymentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(findPaymentByIdUseCase).execute(paymentId);
    }
}
