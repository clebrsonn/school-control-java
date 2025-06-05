package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.usecases.enrollment.CreateEnrollment;
import br.com.hyteck.school_control.models.classrooms.Enrollment; // Added for entity type
import br.com.hyteck.school_control.usecases.enrollment.CreateEnrollment;
import br.com.hyteck.school_control.usecases.enrollment.FindEnrollmentsByStudentId;
import br.com.hyteck.school_control.usecases.enrollment.UpdateEnrollmentMonthlyFee; // Added
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentRequest;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentResponse;
import br.com.hyteck.school_control.web.dtos.classroom.UpdateEnrollmentMonthlyFeeRequestDto;
import io.swagger.v3.oas.annotations.Operation;
// Removed Content for updateInvoiceItemAmount, ensure it's present for updateMonthlyFee
import io.swagger.v3.oas.annotations.media.Content; // Ensure this is present
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
    private final UpdateEnrollmentMonthlyFee updateEnrollmentMonthlyFeeUseCase; // Added

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

    // Removed the erroneous updateInvoiceItemAmount method and its imports/annotations

    @PatchMapping("/{enrollmentId}/monthly-fee")
    @Operation(summary = "Update enrollment monthly fee",
            description = "Updates the monthly fee for a specific enrollment.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Monthly fee updated successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = EnrollmentResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., negative fee)",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Enrollment not found",
                            content = @Content)
            })
    public ResponseEntity<EnrollmentResponse> updateMonthlyFee(
            @PathVariable String enrollmentId,
            @Valid @RequestBody UpdateEnrollmentMonthlyFeeRequestDto requestDto) {

        Enrollment updatedEnrollmentEntity = updateEnrollmentMonthlyFeeUseCase.execute(enrollmentId, requestDto.getMonthlyFee());
        EnrollmentResponse responseDto = EnrollmentResponse.from(updatedEnrollmentEntity); // Using existing static factory method
        return ResponseEntity.ok(responseDto);
    }
}