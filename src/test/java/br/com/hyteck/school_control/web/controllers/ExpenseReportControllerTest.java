package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.usecases.billing.GenerateExpenseReportUseCase;
import br.com.hyteck.school_control.web.dtos.expenses.ExpenseReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ExpenseReportController.class)
class ExpenseReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private GenerateExpenseReportUseCase generateExpenseReportUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    private ExpenseReport expenseReport;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        startDate = LocalDate.of(2023, 1, 1);
        endDate = LocalDate.of(2023, 1, 31);

        expenseReport = ExpenseReport.builder()
                .reportDate(LocalDate.now())
                .totalExpenses(new BigDecimal("1250.99"))
                .details("Monthly expense report for January 2023")
                .build();
    }

    @Test
    void getExpenseReport_ShouldReturnExpenseReport_WhenDatesAreValid() throws Exception {
        // Arrange
        when(generateExpenseReportUseCase.execute(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(expenseReport);

        // Act & Assert
        mockMvc.perform(get("/expense-reports")
                        .param("startDate", startDate.format(DateTimeFormatter.ISO_DATE))
                        .param("endDate", endDate.format(DateTimeFormatter.ISO_DATE))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalExpenses").value(expenseReport.getTotalExpenses().doubleValue()))
                .andExpect(jsonPath("$.details").value(expenseReport.getDetails()));

        verify(generateExpenseReportUseCase).execute(startDate, endDate);
    }

    @Test
    void getExpenseReport_ShouldReturnBadRequest_WhenStartDateIsMissing() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/expense-reports")
                        // Missing startDate
                        .param("endDate", endDate.format(DateTimeFormatter.ISO_DATE))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getExpenseReport_ShouldReturnBadRequest_WhenEndDateIsMissing() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/expense-reports")
                        .param("startDate", startDate.format(DateTimeFormatter.ISO_DATE))
                        // Missing endDate
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getExpenseReport_ShouldReturnBadRequest_WhenDateFormatIsInvalid() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/expense-reports")
                        .param("startDate", "01-01-2023") // Invalid format, expecting ISO_DATE
                        .param("endDate", endDate.format(DateTimeFormatter.ISO_DATE))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
