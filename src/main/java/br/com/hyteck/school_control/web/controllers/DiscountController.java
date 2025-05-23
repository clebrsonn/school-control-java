package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.usecases.discount.*;
import br.com.hyteck.school_control.web.dtos.discount.DiscountRequest;
import br.com.hyteck.school_control.web.dtos.discount.DiscountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/discounts")
@RequiredArgsConstructor
public class DiscountController {

    private final CreateDiscountUseCase createDiscountUseCase;
    private final UpdateDiscountUseCase updateDiscountUseCase;
    private final DeleteDiscountUseCase deleteDiscountUseCase;
    private final FindDiscounts findDiscounts;
    private final FindDiscountById findDiscountById;

    @PostMapping
    public ResponseEntity<DiscountResponse> create(@RequestBody DiscountRequest request) {
        DiscountResponse saved = createDiscountUseCase.execute(request);
        return ResponseEntity.ok((saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiscountResponse> update(@PathVariable String id, @RequestBody DiscountRequest request) {
        DiscountResponse updated = updateDiscountUseCase.execute(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        deleteDiscountUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DiscountResponse>> list(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        Page<DiscountResponse> responses = findDiscounts.execute(pageable);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiscountResponse> getClassRoomById(@PathVariable String id) {
        return findDiscountById.execute(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
