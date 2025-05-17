package br.com.hyteck.school_control.models.payments;

import br.com.hyteck.school_control.models.AbstractModel;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Future;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "discounts")
@Builder
public class Discount extends AbstractModel {
    private String name;
    private String description;
    private BigDecimal value;

    @Future
    private LocalDateTime validateAt;


    @Enumerated(EnumType.STRING)
    private Types type;
}
