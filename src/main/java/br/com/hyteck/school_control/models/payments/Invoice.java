package br.com.hyteck.school_control.models.payments;

import br.com.hyteck.school_control.models.AbstractModel;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*; // Adicionar Lombok

import java.math.BigDecimal;
import java.time.LocalDate; // Usar LocalDate para datas sem hora
import java.time.YearMonth; // Para representar o mês/ano de referência

@Entity
@Table(name = "invoices")
@Getter // Lombok
@Setter // Lombok
@NoArgsConstructor // Lombok
@AllArgsConstructor // Lombok
@Builder // Lombok
public class Invoice extends AbstractModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Uma fatura pertence a uma matrícula
    @JoinColumn(name = "enrollment_id", nullable = false) // Coluna da FK
    @NotNull
    private Enrollment enrollment;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 10, scale = 2) // Precisão para dinheiro
    private BigDecimal amount; // Valor da fatura

    @NotNull
    @FutureOrPresent
    @Column(nullable = false)
    private LocalDate dueDate; // Data de vencimento

    @Column(nullable = false)
    private LocalDate issueDate; // Data de emissão

    @NotNull
    @Column(nullable = false)
    private YearMonth referenceMonth; // Mês e ano de referência (ex: 2024-08)

    @NotNull
    @Enumerated(EnumType.STRING) // Armazena o nome do enum ("PENDING", "PAID")
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    // Relacionamento inverso com Payment (uma fatura pode ter um pagamento)
    @OneToOne(mappedBy = "invoice", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Payment payment; // O pagamento que quitou esta fatura (pode ser null)

    // Outros campos úteis: description, discount, penalty, etc.
    private String description;

}