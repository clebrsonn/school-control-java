package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.services.InvoiceCalculationService;
import br.com.hyteck.school_control.usecases.billing.CountInvoicesByStatus;
import br.com.hyteck.school_control.usecases.billing.GenerateConsolidatedStatementUseCase;
import br.com.hyteck.school_control.usecases.billing.GenerateInvoicesForParents;
import br.com.hyteck.school_control.web.dtos.billing.ConsolidatedStatement;
import br.com.hyteck.school_control.web.dtos.billing.StatementLineItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(BillingController.class)
class BillingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private GenerateConsolidatedStatementUseCase generateStatementUseCase;
    @MockitoBean
    private GenerateInvoicesForParents generateInvoicesForParentsUseCase;
    @MockitoBean
    private CountInvoicesByStatus countInvoicesByStatusUseCase;
    @MockitoBean
    private InvoiceCalculationService invoiceCalculationService;

    @Autowired
    private ObjectMapper objectMapper;

    private ConsolidatedStatement consolidatedStatement;
    private YearMonth testYearMonth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        testYearMonth = YearMonth.of(2023, 10);
        StatementLineItem item = new StatementLineItem("inv1", "Student A", "Monthly Fee", new BigDecimal("100"), LocalDate.now());
        consolidatedStatement = new ConsolidatedStatement(
                "respId1", "Responsible Name", testYearMonth, new BigDecimal("100.00"),
                LocalDate.now().plusDays(10), List.of(item), "http://payment.link", "barcode123"
        );
    }

    @Test
    @WithMockUser // Assumes any authenticated user can access this, or adjust roles if needed
    void getConsolidatedStatementForResponsible_ShouldReturnStatement_WhenFound() throws Exception {
        when(generateStatementUseCase.execute("respId1", testYearMonth)).thenReturn(Optional.of(consolidatedStatement));

        mockMvc.perform(get("/billing/responsibles/{responsibleId}/statements/{yearMonth}", "respId1", testYearMonth.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsibleId").value("respId1"))
                .andExpect(jsonPath("$.totalAmountDue").value(100.00));

        verify(generateStatementUseCase).execute("respId1", testYearMonth);
    }

    @Test
    @WithMockUser
    void getConsolidatedStatementForResponsible_ShouldReturnNotFound_WhenNotFound() throws Exception {
        when(generateStatementUseCase.execute("respId1", testYearMonth)).thenReturn(Optional.empty());

        mockMvc.perform(get("/billing/responsibles/{responsibleId}/statements/{yearMonth}", "respId1", testYearMonth.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(generateStatementUseCase).execute("respId1", testYearMonth);
    }

    @Test
    @WithMockUser // Assumes any authenticated user can access this
    void getConsolidatedStatement_ShouldReturnListOfStatements() throws Exception {
        List<ConsolidatedStatement> statements = Collections.singletonList(consolidatedStatement);
        when(generateStatementUseCase.execute(testYearMonth)).thenReturn(statements);

        mockMvc.perform(get("/billing/statements/{yearMonth}", testYearMonth.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].responsibleId").value("respId1"));

        verify(generateStatementUseCase).execute(testYearMonth);
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void triggerGenerateMonthlyInvoices_ShouldReturnAccepted_WhenUserIsAdmin() throws Exception {
        doNothing().when(generateInvoicesForParentsUseCase).execute(testYearMonth);

        mockMvc.perform(post("/billing/generate-monthly-invoices/{yearMonth}", testYearMonth.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        verify(generateInvoicesForParentsUseCase).execute(testYearMonth);
    }
    
    @Test
    @WithMockUser(roles = {"USER"}) // Non-admin user
    void triggerGenerateMonthlyInvoices_ShouldReturnForbidden_WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(post("/billing/generate-monthly-invoices/{yearMonth}", testYearMonth.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(generateInvoicesForParentsUseCase);
    }


    @Test
    @WithMockUser(roles = {"ADMIN"})
    void countInvoicesByStatus_ShouldReturnCount_WhenUserIsAdmin() throws Exception {
        InvoiceStatus status = InvoiceStatus.PENDING;
        long count = 15L;
        when(countInvoicesByStatusUseCase.execute(status)).thenReturn(count);

        mockMvc.perform(get("/billing/invoices/{status}/count", status.name())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(count)));

        verify(countInvoicesByStatusUseCase).execute(status);
    }

    @Test
    @WithMockUser // Assuming any authenticated user can access this, adjust if ADMIN only
    void getTotalToReceive_ShouldReturnTotalAmount() throws Exception {
        BigDecimal total = new BigDecimal("2500.50");
        when(invoiceCalculationService.calcularTotalAReceberNoMes(testYearMonth)).thenReturn(total);

        mockMvc.perform(get("/billing/total-month/{referenceMonth}", testYearMonth.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(total.doubleValue()));

        verify(invoiceCalculationService).calcularTotalAReceberNoMes(testYearMonth);
    }
}
