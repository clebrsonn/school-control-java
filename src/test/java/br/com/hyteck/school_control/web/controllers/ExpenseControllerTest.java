package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.models.expenses.Expense;
import br.com.hyteck.school_control.usecases.expenses.CreateExpenseUseCase;
import br.com.hyteck.school_control.usecases.expenses.ListExpensesUseCase;
import br.com.hyteck.school_control.usecases.expenses.UpdateExpenseUseCase;
import br.com.hyteck.school_control.web.dtos.expenses.ExpenseRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ExpenseController.class)
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private CreateExpenseUseCase createExpenseUseCase;

    @MockitoBean
    private UpdateExpenseUseCase updateExpenseUseCase;

    @MockitoBean
    private ListExpensesUseCase listExpensesUseCase;

    @Autowired
    private ObjectMapper objectMapper; // For general JSON conversion if needed, though not for multipart params directly

    private Expense sampleExpense;
    private MockMultipartFile mockReceiptFile;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        sampleExpense = Expense.builder()
                .id("expenseId123")
                .description("Office Supplies")
                .date(LocalDate.now())
                .value(new BigDecimal("150.00"))
                .receiptUrl("http://example.com/receipt.jpg")
                .build();

        mockReceiptFile = new MockMultipartFile(
                "receipt", // a_underscore_parameter_name_that_matches_the_request_part_name
                "receipt.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "receipt_content".getBytes(StandardCharsets.UTF_8)
        );
    }

    @Test
    void createExpense_ShouldReturnCreatedExpense_WhenRequestIsValid() throws Exception {
        // Arrange
        when(createExpenseUseCase.execute(any(ExpenseRequest.class))).thenReturn(sampleExpense);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.multipart("/expenses")
                        .file(mockReceiptFile)
                        .param("value", "150.00")
                        .param("date", LocalDate.now().toString())
                        .param("description", "Office Supplies")
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk()) // Controller returns ResponseEntity.ok()
                .andExpect(jsonPath("$.id").value(sampleExpense.getId()))
                .andExpect(jsonPath("$.description").value(sampleExpense.getDescription()));

        verify(createExpenseUseCase).execute(any(ExpenseRequest.class));
    }

    @Test
    void updateExpense_ShouldReturnUpdatedExpense_WhenRequestIsValid() throws Exception {
        // Arrange
        String expenseId = "expenseId123";
        Expense updatedExpense = Expense.builder()
                .id(expenseId)
                .description("Updated Office Supplies")
                .date(sampleExpense.getDate())
                .value(sampleExpense.getValue())
                .receiptUrl(sampleExpense.getReceiptUrl())
                .build();
        when(updateExpenseUseCase.execute(eq(expenseId), any(ExpenseRequest.class))).thenReturn(updatedExpense);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.multipart("/expenses/{id}", expenseId)
                        .file(mockReceiptFile)
                        .param("value", "150.00")
                        .param("date", LocalDate.now().toString())
                        .param("description", "Updated Office Supplies")
                        .with(request -> { // Needed to set method to PUT for multipart
                            request.setMethod("PUT");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(updatedExpense.getId()))
                .andExpect(jsonPath("$.description").value(updatedExpense.getDescription()));

        verify(updateExpenseUseCase).execute(eq(expenseId), any(ExpenseRequest.class));
    }


    @Test
    void listAllExpenses_ShouldReturnPageOfExpenses() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Expense> expenseList = Collections.singletonList(sampleExpense);
        Page<Expense> expensePage = new PageImpl<>(expenseList, pageable, expenseList.size());

        when(listExpensesUseCase.execute(any(Pageable.class))).thenReturn(expensePage);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/expenses")
                        .param("page", String.valueOf(pageable.getPageNumber()))
                        .param("size", String.valueOf(pageable.getPageSize()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(sampleExpense.getId()))
                .andExpect(jsonPath("$.totalElements").value(expenseList.size()));

        verify(listExpensesUseCase).execute(any(Pageable.class));
    }
}
