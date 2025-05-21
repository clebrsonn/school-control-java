package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.models.payments.Types;
import br.com.hyteck.school_control.usecases.discount.*;
import br.com.hyteck.school_control.web.dtos.discount.DiscountRequest;
import br.com.hyteck.school_control.web.dtos.discount.DiscountResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DiscountControllerTest {
    private CreateDiscountUseCase createDiscountUseCase;
    private UpdateDiscountUseCase updateDiscountUseCase;
    private DeleteDiscountUseCase deleteDiscountUseCase;
    private FindDiscounts findDiscounts;
    private FindDiscountById findDiscountById;
    private DiscountController discountController;

    @BeforeEach
    void setUp() {
        createDiscountUseCase = mock(CreateDiscountUseCase.class);
        updateDiscountUseCase = mock(UpdateDiscountUseCase.class);
        deleteDiscountUseCase = mock(DeleteDiscountUseCase.class);
        findDiscounts = mock(FindDiscounts.class);
        findDiscountById = mock(FindDiscountById.class);
        discountController = new DiscountController(
                createDiscountUseCase,
                updateDiscountUseCase,
                deleteDiscountUseCase,
                findDiscounts,
                findDiscountById
        );
    }

    @Test
    void shouldReturnDiscountResponseWhenCreateSuccess() {
        DiscountRequest request = new DiscountRequest(
            "Desconto de Aniversário", // name
            "Desconto válido para aniversariantes", // description
            BigDecimal.valueOf(10), // value
            LocalDateTime.now(), // validateAt
            Types.MENSALIDADE // type (ajuste conforme enum)
        );
        DiscountResponse response = new DiscountResponse("id1", "Desconto de Aniversário","Desconto válido para aniversariantes",  BigDecimal.valueOf(10), LocalDateTime.now(), Types.MENSALIDADE);
        when(createDiscountUseCase.execute(any(DiscountRequest.class))).thenReturn(response);
        ResponseEntity<DiscountResponse> result = discountController.create(request);
        assertNotNull(result.getBody());
        assertEquals(response, result.getBody());
    }

    @Test
    void shouldThrowExceptionWhenCreateFails() {
        DiscountRequest request = new DiscountRequest(
            "Desconto de Aniversário",
            "Desconto válido para aniversariantes",
            BigDecimal.valueOf(10),
            LocalDateTime.now(),
            Types.MENSALIDADE
        );
        when(createDiscountUseCase.execute(any(DiscountRequest.class))).thenThrow(new RuntimeException("error"));
        assertThrows(RuntimeException.class, () -> discountController.create(request));
    }

    @Test
    void shouldReturnDiscountResponseWhenUpdateSuccess() {
        DiscountRequest request = new DiscountRequest(
            "Desconto de Aniversário",
            "Desconto válido para aniversariantes",
            BigDecimal.valueOf(10),
            LocalDateTime.now(),
            Types.MENSALIDADE
        );
        DiscountResponse response = new DiscountResponse("id1", "Desconto de Aniversário", "Desconto válido para aniversariantes", BigDecimal.valueOf(10), LocalDateTime.now(), Types.MENSALIDADE);
        when(updateDiscountUseCase.execute(eq("id1"), any(DiscountRequest.class))).thenReturn(response);
        ResponseEntity<DiscountResponse> result = discountController.update("id1", request);
        assertNotNull(result.getBody());
        assertEquals(response, result.getBody());
    }

    @Test
    void shouldThrowExceptionWhenUpdateFails() {
        DiscountRequest request = new DiscountRequest(
            "Desconto de Aniversário",
            "Desconto válido para aniversariantes",
            BigDecimal.valueOf(10),
            LocalDateTime.now(),
            Types.MENSALIDADE
        );
        when(updateDiscountUseCase.execute(eq("id1"), any(DiscountRequest.class))).thenThrow(new RuntimeException("error"));
        assertThrows(RuntimeException.class, () -> discountController.update("id1", request));
    }

    @Test
    void shouldReturnNoContentWhenDeleteSuccess() {
        doNothing().when(deleteDiscountUseCase).execute("id1");
        ResponseEntity<Void> result = discountController.delete("id1");
        assertEquals(204, result.getStatusCodeValue());
        assertNull(result.getBody());
    }

    @Test
    void shouldThrowExceptionWhenDeleteFails() {
        doThrow(new RuntimeException("error")).when(deleteDiscountUseCase).execute("id1");
        assertThrows(RuntimeException.class, () -> discountController.delete("id1"));
    }

    @Test
    void shouldReturnPageOfDiscountsWhenList() {
        Pageable pageable = PageRequest.of(0, 20);
        DiscountResponse response = new DiscountResponse("id1", "Desconto de Aniversário", "Desconto válido para aniversariantes", BigDecimal.valueOf(10), LocalDateTime.now(), Types.MENSALIDADE);
        Page<DiscountResponse> page = new PageImpl<>(List.of(response), pageable, 1);
        when(findDiscounts.execute(pageable)).thenReturn(page);
        ResponseEntity<Page<DiscountResponse>> result = discountController.list(pageable);
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getTotalElements());
        assertEquals(response, result.getBody().getContent().get(0));
    }

    @Test
    void shouldReturnDiscountWhenGetByIdFound() {
        DiscountResponse response = new DiscountResponse("id1", "Desconto de Aniversário", "Desconto válido para aniversariantes", BigDecimal.valueOf(10), LocalDateTime.now(), Types.MENSALIDADE);
        when(findDiscountById.execute("id1")).thenReturn(Optional.of(response));
        ResponseEntity<DiscountResponse> result = discountController.getClassRoomById("id1");
        assertNotNull(result.getBody());
        assertEquals(response, result.getBody());
        assertEquals(200, result.getStatusCodeValue());
    }

    @Test
    void shouldReturnNotFoundWhenGetByIdNotFound() {
        when(findDiscountById.execute("id1")).thenReturn(Optional.empty());
        ResponseEntity<DiscountResponse> result = discountController.getClassRoomById("id1");
        assertNull(result.getBody());
        assertEquals(404, result.getStatusCodeValue());
    }
}

