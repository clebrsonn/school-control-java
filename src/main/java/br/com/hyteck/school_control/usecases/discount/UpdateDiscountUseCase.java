package br.com.hyteck.school_control.usecases.discount;

import br.com.hyteck.school_control.models.payments.Discount;
import br.com.hyteck.school_control.repositories.DiscountRepository;
import br.com.hyteck.school_control.web.dtos.discount.DiscountRequest;
import br.com.hyteck.school_control.web.dtos.discount.DiscountResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@AllArgsConstructor
public class UpdateDiscountUseCase {

    private final DiscountRepository discountRepository;

    public DiscountResponse execute(String id, @Valid  DiscountRequest discount) {
        if (!discountRepository.existsById(id)) {
            throw new IllegalArgumentException("Discount not found");
        }
        return DiscountResponse.from(discountRepository.save(discount.to()));
    }
}
