package br.com.hyteck.school_control.usecases.discount;

import br.com.hyteck.school_control.repositories.DiscountRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class DeleteDiscountUseCase {

    @Autowired
    private DiscountRepository discountRepository;

    public void execute(String id) {
        if (!discountRepository.existsById(id)) {
            throw new IllegalArgumentException("Discount not found");
        }
        discountRepository.deleteById(id);
    }
}
