package br.com.hyteck.school_control.web.dtos.classroom;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class UpdateEnrollmentMonthlyFeeRequestDto {

    @NotNull(message = "Monthly fee cannot be null")
    @PositiveOrZero(message = "Monthly fee must be a positive value or zero")
    private BigDecimal monthlyFee;
}
