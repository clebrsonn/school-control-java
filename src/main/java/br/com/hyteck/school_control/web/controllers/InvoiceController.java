package br.com.hyteck.school_control.web.controllers;

// Removed InvoiceItem, Enrollment, InvoiceService, DTOs, Operation, Content, Schema, ApiResponse
// Removed Valid, ResponseEntity, PathVariable, RequestBody, BigDecimal
import io.swagger.v3.oas.annotations.tags.Tag;
// import lombok.AllArgsConstructor; // Removed as no fields remain
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/invoices")
// @AllArgsConstructor // Removed
@Tag(name = "Invoices", description = "Endpoints for managing invoices and invoice items")
public class InvoiceController {

    // private final InvoiceService invoiceService; // Removed

    // updateInvoiceItemAmount method removed
    // mapToInvoiceItemResponseDto method removed
}
