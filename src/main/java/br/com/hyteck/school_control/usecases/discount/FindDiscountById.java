package br.com.hyteck.school_control.usecases.discount;

import br.com.hyteck.school_control.repositories.DiscountRepository;
import br.com.hyteck.school_control.web.dtos.discount.DiscountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class FindDiscountById {

    private final DiscountRepository discountRepository;

    @Transactional(readOnly = true)
    public Optional<DiscountResponse> execute(String id) {
        log.info("Buscando desconto com o id: {}", id);
        return discountRepository.findById(id)
                .map(DiscountResponse::from); // Mapeia para DTO se encontrado
    }

}
