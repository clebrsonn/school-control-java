package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.models.payments.Types;
import br.com.hyteck.school_control.usecases.discount.*;
import br.com.hyteck.school_control.web.dtos.discount.DiscountRequest;
import br.com.hyteck.school_control.web.dtos.discount.DiscountResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(DiscountController.class)
class DiscountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private CreateDiscountUseCase createDiscountUseCase;
    @MockitoBean
    private UpdateDiscountUseCase updateDiscountUseCase;
    @MockitoBean
    private DeleteDiscountUseCase deleteDiscountUseCase;
    @MockitoBean
    private FindDiscounts findDiscountsUseCase;
    @MockitoBean
    private FindDiscountById findDiscountByIdUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    private DiscountRequest discountRequest;
    private DiscountResponse discountResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        discountRequest = new DiscountRequest(
                "Summer Discount",
                "10% off for summer courses",
                new BigDecimal("10.00"),
                LocalDateTime.now().plusMonths(1),
                Types.MENSALIDADE
        );

        discountResponse = new DiscountResponse(
                "discId123",
                "Summer Discount",
                "10% off for summer courses",
                new BigDecimal("10.00"),
                LocalDateTime.now().plusMonths(1),
                Types.MENSALIDADE
        );
    }

    @Test
    void create_ShouldReturnDiscountResponse_WhenRequestIsValid() throws Exception {
        // Arrange
        when(createDiscountUseCase.execute(any(DiscountRequest.class))).thenReturn(discountResponse);

        // Act & Assert
        mockMvc.perform(post("/discounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(discountRequest)))
                .andExpect(status().isOk()) // Expect 200 OK as per controller
                .andExpect(jsonPath("$.id").value(discountResponse.id()))
                .andExpect(jsonPath("$.name").value(discountResponse.name()));

        verify(createDiscountUseCase).execute(any(DiscountRequest.class));
    }

    @Test
    void update_ShouldReturnUpdatedDiscountResponse_WhenRequestIsValid() throws Exception {
        // Arrange
        String discountId = "discId123";
        DiscountResponse updatedResponse = new DiscountResponse(
                discountId,
                "Winter Discount", // Updated name
                discountRequest.description(),
                discountRequest.value(),
                discountRequest.validateAt(),
                discountRequest.type()
        );
        when(updateDiscountUseCase.execute(any(String.class), any(DiscountRequest.class))).thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/discounts/{id}", discountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(discountRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(discountId))
                .andExpect(jsonPath("$.name").value(updatedResponse.name()));

        verify(updateDiscountUseCase).execute(any(String.class), any(DiscountRequest.class));
    }

    @Test
    void delete_ShouldReturnNoContent_WhenDiscountExists() throws Exception {
        // Arrange
        String discountId = "discId123";
        doNothing().when(deleteDiscountUseCase).execute(discountId);

        // Act & Assert
        mockMvc.perform(delete("/discounts/{id}", discountId))
                .andExpect(status().isNoContent());

        verify(deleteDiscountUseCase).execute(discountId);
    }

    @Test
    void list_ShouldReturnPageOfDiscountResponses() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<DiscountResponse> discountList = Collections.singletonList(discountResponse);
        Page<DiscountResponse> discountPage = new PageImpl<>(discountList, pageable, discountList.size());

        when(findDiscountsUseCase.execute(any(Pageable.class))).thenReturn(discountPage);

        // Act & Assert
        mockMvc.perform(get("/discounts")
                        .param("page", String.valueOf(pageable.getPageNumber()))
                        .param("size", String.valueOf(pageable.getPageSize()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(discountResponse.id()))
                .andExpect(jsonPath("$.totalElements").value(discountList.size()));

        verify(findDiscountsUseCase).execute(any(Pageable.class));
    }

    @Test
    void getDiscountById_ShouldReturnDiscountResponse_WhenDiscountExists() throws Exception {
        // Arrange
        String discountId = "discId123";
        when(findDiscountByIdUseCase.execute(discountId)).thenReturn(Optional.of(discountResponse));

        // Act & Assert
        mockMvc.perform(get("/discounts/{id}", discountId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(discountResponse.id()))
                .andExpect(jsonPath("$.name").value(discountResponse.name()));

        verify(findDiscountByIdUseCase).execute(discountId);
    }

    @Test
    void getDiscountById_ShouldReturnNotFound_WhenDiscountDoesNotExist() throws Exception {
        // Arrange
        String discountId = "nonExistentId";
        when(findDiscountByIdUseCase.execute(discountId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/discounts/{id}", discountId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(findDiscountByIdUseCase).execute(discountId);
    }
}
