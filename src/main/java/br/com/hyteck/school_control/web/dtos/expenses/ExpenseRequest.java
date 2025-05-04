package br.com.hyteck.school_control.web.dtos.expenses;

import br.com.hyteck.school_control.models.expenses.Expense;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest (BigDecimal value,
                              LocalDate date,
                              String description,

                              MultipartFile receipt){

    public static Expense to(ExpenseRequest expenseRequest){
        return Expense.builder()
                .description(expenseRequest.description())
                .date(expenseRequest.date())
                .value(expenseRequest.value())
                .build();
    }
}
