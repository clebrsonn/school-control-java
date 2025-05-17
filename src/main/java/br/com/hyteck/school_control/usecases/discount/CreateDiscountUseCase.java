package br.com.hyteck.school_control.usecases.discount;

import br.com.hyteck.school_control.models.payments.Discount;
import br.com.hyteck.school_control.repositories.DiscountRepository;
import br.com.hyteck.school_control.web.dtos.discount.DiscountRequest;
import br.com.hyteck.school_control.web.dtos.discount.DiscountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@RequiredArgsConstructor
public class CreateDiscountUseCase {

    private final DiscountRepository discountRepository;

    public DiscountResponse execute(DiscountRequest discountDTO) {

        Discount discount = discountDTO.to();

        return DiscountResponse.from(discountRepository.save(discount));
    }
}
