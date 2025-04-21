package br.com.hyteck.school_control.models.expenses;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.Currency;

@Entity
public class Expense {
    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    private String id;

    private Currency total;

    @NotBlank
    private LocalDateTime date;

    @NotBlank
    private String description;
    @NotBlank
    private String receiptUrl;

}
