package br.com.hyteck.school_control.usecases.discount;

import br.com.hyteck.school_control.repositories.DiscountRepository;
import br.com.hyteck.school_control.web.dtos.discount.DiscountResponse;
import br.com.hyteck.school_control.web.dtos.responsible.ResponsibleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class FindDiscounts {


    private final DiscountRepository discountRepository;

    @Transactional(readOnly = true)
    public Page<DiscountResponse> execute(Pageable pageable) {
        log.info("Buscando todos os descontos paginados: {}", pageable);
        return discountRepository.findAll(pageable)
                .map(DiscountResponse::from); // Mapeia para DTO se encontrado
    }

}
