package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleRequest;
import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleResponse;
import br.com.hyteck.school_control.usecases.responsible.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/responsibles")
public class ResponsibleController {

    // Injetar todos os Use Cases necessários
    private final CreateResponsible createResponsibleUseCase;
    private final FindResponsibleById findResponsibleByIdUseCase;
    private final FindResponsibles findAllResponsiblesUseCase;
    private final UpdateResponsible updateResponsibleUseCase;
    private final DeleteResponsible deleteResponsibleUseCase;

    public ResponsibleController(CreateResponsible createResponsibleUseCase, FindResponsibleById findResponsibleByIdUseCase, FindResponsibles findAllResponsiblesUseCase, UpdateResponsible updateResponsibleUseCase, DeleteResponsible deleteResponsibleUseCase) {
        this.createResponsibleUseCase = createResponsibleUseCase;
        this.findResponsibleByIdUseCase = findResponsibleByIdUseCase;
        this.findAllResponsiblesUseCase = findAllResponsiblesUseCase;
        this.updateResponsibleUseCase = updateResponsibleUseCase;
        this.deleteResponsibleUseCase = deleteResponsibleUseCase;
    }

    // Construtor com todas as injeções

    // --- CREATE ---
    @PostMapping
    public ResponseEntity<ResponsibleResponse> createResponsible(
            @Valid @RequestBody ResponsibleRequest requestDTO) {
        ResponsibleResponse createdResponsible = createResponsibleUseCase.execute(requestDTO);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdResponsible.id())
                .toUri();
        return ResponseEntity.created(location).body(createdResponsible);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponsibleResponse> getResponsibleById(@PathVariable String id) {
        // O Use Case retorna Optional, o map/orElseGet lida com o 404
        return findResponsibleByIdUseCase.execute(id)
                .map(ResponseEntity::ok) // Se presente, retorna 200 OK com o corpo
                .orElseGet(() -> ResponseEntity.notFound().build()); // Se vazio, retorna 404 Not Found
    }

    @GetMapping
    public ResponseEntity<Page<ResponsibleResponse>> getAllResponsibles(
            // Define valores padrão para paginação e permite sobrescrever via query params (?page=1&size=5&sort=name,asc)
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        Page<ResponsibleResponse> responsiblePage = findAllResponsiblesUseCase.execute(pageable);
        return ResponseEntity.ok(responsiblePage); // Retorna 200 OK com a página
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponsibleResponse> updateResponsible(
            @PathVariable String id,
            @Valid @RequestBody ResponsibleRequest requestDTO) {
        // O Use Case já lida com o 'NotFound' lançando exceção (que será tratada pelo ControllerAdvice)
        // ou você pode fazer como o GET by ID se o use case retornasse Optional
        ResponsibleResponse updatedResponsible = updateResponsibleUseCase.execute(id, requestDTO);
        return ResponseEntity.ok(updatedResponsible); // Retorna 200 OK com o responsável atualizado
    }

    // --- DELETE ---
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Retorna 204 No Content em caso de sucesso
    public void deleteResponsible(@PathVariable String id) {
        // O Use Case lida com o 'NotFound' lançando exceção
        deleteResponsibleUseCase.execute(id);
        // Nenhum corpo é retornado no 204
    }
}