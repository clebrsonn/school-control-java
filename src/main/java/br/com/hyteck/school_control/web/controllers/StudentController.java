package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.usecases.student.*;
import br.com.hyteck.school_control.web.dtos.student.StudentRequest;
import br.com.hyteck.school_control.web.dtos.student.StudentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/students")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class StudentController {

    private final CreateStudent createStudentUseCase;
    private final FindStudentById findStudentByIdUseCase;
    private final FindStudents findAllStudentsUseCase;
    private final UpdateStudent updateStudentUseCase;
    private final DeleteStudent deleteStudentUseCase;


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentRequest requestDTO) {
        StudentResponse createdStudent = createStudentUseCase.execute(requestDTO);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest() // Empieza desde /students
                .path("/{id}")        // Añade /<id>
                .buildAndExpand(createdStudent.id()) // Reemplaza {id} con el ID real
                .toUri();             // Convierte a URI

        return ResponseEntity.created(location).body(createdStudent);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public void createStudent(@Valid @RequestBody List<StudentRequest> requestDTOs) {
        for (StudentRequest requestDTO : requestDTOs) {
            createStudentUseCase.execute(requestDTO);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable String id) {
        return findStudentByIdUseCase.execute(id)
                .map(ResponseEntity::ok) // Se encontrou, retorna 200 OK com o corpo
                .orElseGet(() -> ResponseEntity.notFound().build()); // Se não, retorna 404 Not Found
    }

    // GET (Listar todos com paginação)
    @GetMapping
    public ResponseEntity<Page<StudentResponse>> getAllStudents(
            @PageableDefault(size = 10, sort = "name") Pageable pageable) { // Define padrões de paginação
        Page<StudentResponse> studentPage = findAllStudentsUseCase.execute(pageable);
        return ResponseEntity.ok(studentPage);
    }

    // PUT (Atualizar)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable String id,
            @Valid @RequestBody StudentRequest requestDTO) {
        StudentResponse updatedStudent = updateStudentUseCase.execute(id, requestDTO);
        return ResponseEntity.ok(updatedStudent); // Retorna 200 OK com o estudante atualizado
    }

    // DELETE (Excluir)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteStudent(@PathVariable String id) {
        deleteStudentUseCase.execute(id);
    }


}