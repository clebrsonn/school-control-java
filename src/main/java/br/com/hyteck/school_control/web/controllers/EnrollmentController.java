package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.usecases.enrollment.CreateEnrollment;
import br.com.hyteck.school_control.usecases.enrollment.FindEnrollmentsByStudentId;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentRequest;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentResponse;
import br.com.hyteck.school_control.models.payments.InvoiceItem;
import br.com.hyteck.school_control.services.InvoiceService;
import br.com.hyteck.school_control.web.dtos.invoice.InvoiceItemResponseDto;
import br.com.hyteck.school_control.web.dtos.invoice.InvoiceItemUpdateRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final CreateEnrollment createEnrollmentUseCase;
    private final FindEnrollmentsByStudentId findEnrollmentsByStudentId;
    private final InvoiceService invoiceService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnrollmentResponse> enrollStudent(@Valid @RequestBody EnrollmentRequest requestDTO) {
        EnrollmentResponse createdEnrollment = createEnrollmentUseCase.execute(requestDTO);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdEnrollment.id())
                .toUri();

        return ResponseEntity.created(location).body(createdEnrollment);
    }

    //TODO: implementar o securityService.isStudentOwner
    //TODO: Ajustar o responsável, pois ele vai ser um tipo de usuário do sistema, e não uma entidade separada
    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isStudentOwner(authentication, #studentId)")
    public ResponseEntity<List<EnrollmentResponse>> getStudentEnrollments(@PathVariable String studentId) {
        List<EnrollmentResponse> enrollments = findEnrollmentsByStudentId.execute(studentId);
        if (enrollments.isEmpty()) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok(enrollments);
        }
    }

    @PatchMapping("/{enrollmentId}/invoice-items/{itemId}")
    @Operation(summary = "Update an invoice item's amount for a specific enrollment",
            description = "Updates the financial amount of a specific item associated with an enrollment's invoice line items.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invoice item updated successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = InvoiceItemResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input data",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Invoice item not found",
                            content = @Content)
            })
    public ResponseEntity<InvoiceItemResponseDto> updateInvoiceItemAmount(
            @PathVariable String enrollmentId,
            @PathVariable String itemId,
            @Valid @RequestBody InvoiceItemUpdateRequestDto requestDto) {

        // enrollmentId is available for future use (e.g., validation, logging)
        // For now, the service call remains the same as it was in InvoiceController
        InvoiceItem updatedInvoiceItem = invoiceService.updateInvoiceItemAmount(itemId, requestDto.getNewAmount());
        InvoiceItemResponseDto responseDto = mapToInvoiceItemResponseDto(updatedInvoiceItem);
        return ResponseEntity.ok(responseDto);
    }

    // Manual mapper
    private InvoiceItemResponseDto mapToInvoiceItemResponseDto(InvoiceItem invoiceItem) {
        if (invoiceItem == null) {
            return null;
        }
        return InvoiceItemResponseDto.builder()
                .id(invoiceItem.getId())
                .description(invoiceItem.getDescription())
                .amount(invoiceItem.getAmount())
                .type(invoiceItem.getType())
                .enrollmentId(invoiceItem.getEnrollment() != null ? invoiceItem.getEnrollment().getId() : null)
                .build();
    }
}