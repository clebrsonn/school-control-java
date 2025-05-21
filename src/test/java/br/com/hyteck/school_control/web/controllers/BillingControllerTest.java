package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.services.InvoiceCalculationService;
import br.com.hyteck.school_control.usecases.billing.CountInvoicesByStatus;
import br.com.hyteck.school_control.usecases.billing.GenerateConsolidatedStatementUseCase;
import br.com.hyteck.school_control.usecases.billing.GenerateInvoicesForParents;
import br.com.hyteck.school_control.web.dtos.billing.ConsolidatedStatement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillingControllerTest {
    private GenerateConsolidatedStatementUseCase generateStatementUseCase;
    private GenerateInvoicesForParents generateInvoicesForParents;
    private CountInvoicesByStatus countInvoicesByStatus;
    private InvoiceCalculationService invoiceCalculationService;
    private BillingController billingController;

    @BeforeEach
    void setUp() {
        generateStatementUseCase = mock(GenerateConsolidatedStatementUseCase.class);
        generateInvoicesForParents = mock(GenerateInvoicesForParents.class);
        countInvoicesByStatus = mock(CountInvoicesByStatus.class);
        invoiceCalculationService = mock(InvoiceCalculationService.class);
        billingController = new BillingController(
                generateStatementUseCase,
                generateInvoicesForParents,
                countInvoicesByStatus,
                invoiceCalculationService
        );
    }

    @Test
    void getConsolidatedStatementForResponsible_shouldReturnStatement_whenExists() {
        ConsolidatedStatement statement = mock(ConsolidatedStatement.class);
        when(generateStatementUseCase.execute(eq("resp1"), any(YearMonth.class)))
                .thenReturn(Optional.of(statement));
        ResponseEntity<ConsolidatedStatement> response = billingController.getConsolidatedStatementForResponsible("resp1", YearMonth.of(2025, 5));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(statement, response.getBody());
    }

    @Test
    void getConsolidatedStatementForResponsible_shouldReturnNotFound_whenNotExists() {
        when(generateStatementUseCase.execute(eq("resp1"), any(YearMonth.class)))
                .thenReturn(Optional.empty());
        ResponseEntity<ConsolidatedStatement> response = billingController.getConsolidatedStatementForResponsible("resp1", YearMonth.of(2025, 5));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getConsolidatedStatement_shouldReturnList() {
        ConsolidatedStatement statement = mock(ConsolidatedStatement.class);
        when(generateStatementUseCase.execute(any(YearMonth.class)))
                .thenReturn(List.of(statement));
        ResponseEntity<List<ConsolidatedStatement>> response = billingController.getConsolidatedStatement(YearMonth.of(2025, 5));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void triggerGenerateMonthlyInvoices_shouldReturnAccepted() {
        ResponseEntity<Void> response = billingController.triggerGenerateMonthlyInvoices(YearMonth.of(2025, 5));
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(generateInvoicesForParents).execute(any(YearMonth.class));
    }

    @Test
    void countInvoicesByStatus_shouldReturnCount() {
        when(countInvoicesByStatus.execute(any())).thenReturn(5L);
        ResponseEntity<Long> response = billingController.countInvoicesByStatus(br.com.hyteck.school_control.models.payments.InvoiceStatus.PENDING);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5L, response.getBody());
    }

    @Test
    void getTotalToReceive_shouldReturnTotal() {
        when(invoiceCalculationService.calcularTotalAReceberNoMes(any(YearMonth.class))).thenReturn(BigDecimal.TEN);
        ResponseEntity<BigDecimal> response = billingController.getTotalToReceive(YearMonth.of(2025, 5));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(BigDecimal.TEN, response.getBody());
    }
}

