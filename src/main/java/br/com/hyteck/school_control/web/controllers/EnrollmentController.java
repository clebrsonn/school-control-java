package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.usecases.enrollment.CreateEnrollment;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentRequest;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/enrollments") // Endpoint base para matrículas
public class EnrollmentController {

    private final CreateEnrollment createEnrollmentUseCase;

    public EnrollmentController(CreateEnrollment createEnrollmentUseCase) {
        this.createEnrollmentUseCase = createEnrollmentUseCase;
    }

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

    // Adicionar outros endpoints (DELETE /enrollments/{id}, GET /enrollments, GET /students/{id}/enrollments, etc.)
    // ...

}