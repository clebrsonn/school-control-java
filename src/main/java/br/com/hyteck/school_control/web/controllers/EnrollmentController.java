package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.usecases.enrollment.CreateEnrollment;
import br.com.hyteck.school_control.usecases.enrollment.FindEnrollmentsByStudentId;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentRequest;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentResponse;
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

}