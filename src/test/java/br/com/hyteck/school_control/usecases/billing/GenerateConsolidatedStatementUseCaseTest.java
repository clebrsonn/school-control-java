package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import br.com.hyteck.school_control.web.dtos.billing.ConsolidatedStatement;
import br.com.hyteck.school_control.web.dtos.billing.StatementLineItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Habilita a integração do Mockito com JUnit 5
class GenerateConsolidatedStatementUseCaseTest {

    @Mock // Cria um mock para o InvoiceRepository
    private InvoiceRepository invoiceRepository;

    @Mock // Cria um mock para o ResponsibleRepository
    private ResponsibleRepository responsibleRepository;

    @InjectMocks // Cria uma instância do UseCase e injeta os mocks (@Mock) nele
    private GenerateConsolidatedStatementUseCase generateConsolidatedStatementUseCase;

    private String responsibleId;
    private YearMonth referenceMonth;
    private Responsible responsible;
    private Student student1;
    private Student student2;
    private ClassRoom classRoom1;
    private Enrollment enrollment1;
    private Enrollment enrollment2;
    private Invoice invoice1;
    private Invoice invoice2;

    @BeforeEach
    void setUp() {
        // Configuração inicial comum para os testes
        responsibleId = "resp-123";
        referenceMonth = YearMonth.of(2024, 8);

        responsible = Responsible.builder()
                .id(responsibleId)
                .name("João da Silva")
                .build();

        student1 = Student.builder().id("stud-001").name("Pedro Silva").responsible(responsible).build();
        student2 = Student.builder().id("stud-002").name("Maria Silva").responsible(responsible).build();
        classRoom1 = ClassRoom.builder().id("class-A").name("Turma A").year("2024").build();

        enrollment1 = Enrollment.builder().id("enr-01").student(student1).classroom(classRoom1).build();
        enrollment2 = Enrollment.builder().id("enr-02").student(student2).classroom(classRoom1).build();

        invoice1 = Invoice.builder()
                .id("inv-001")
                .enrollment(enrollment1)
                .description("Mensalidade Turma A")
                .amount(new BigDecimal("500.00"))
                .dueDate(LocalDate.from(referenceMonth.atDay(10).atTime(23, 59))) // Ex: 2024-08-10 23:59
                .issueDate(LocalDate.from(referenceMonth.atDay(1).atStartOfDay()))
                .status(InvoiceStatus.PENDING)
                .referenceMonth(referenceMonth)
                .build();

        invoice2 = Invoice.builder()
                .id("inv-002")
                .enrollment(enrollment2)
                .description("Taxa Matrícula")
                .amount(new BigDecimal("150.50"))
                .dueDate(LocalDate.from(referenceMonth.atDay(5).atTime(23, 59))) // Ex: 2024-08-05 23:59
                .issueDate(LocalDate.from(referenceMonth.atDay(1).atStartOfDay()))
                .status(InvoiceStatus.PENDING) // Pode ser OVERDUE também
                .referenceMonth(referenceMonth)
                .build();
    }

    @Test
    @DisplayName("Deve retornar extrato consolidado quando responsável e faturas pendentes existem")
    void execute_shouldReturnConsolidatedStatement_whenResponsibleAndInvoicesExist() {
        // Arrange (Organizar)
        List<Invoice> pendingInvoices = List.of(invoice1, invoice2);
        List<InvoiceStatus> expectedStatuses = List.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);

        // Configura o mock do responsibleRepository para retornar o responsável
        when(responsibleRepository.findById(responsibleId)).thenReturn(Optional.of(responsible));

        // Configura o mock do invoiceRepository para retornar as faturas pendentes
        when(invoiceRepository.findPendingInvoicesByResponsibleAndMonth(responsibleId, referenceMonth, expectedStatuses))
                .thenReturn(pendingInvoices);

        // Act (Agir)
        Optional<ConsolidatedStatement> result = generateConsolidatedStatementUseCase.execute(responsibleId, referenceMonth);

        // Assert (Verificar)
        assertThat(result).isPresent(); // Verifica se o Optional não está vazio

        ConsolidatedStatement statement = result.get();
        assertThat(statement.responsibleId()).isEqualTo(responsibleId);
        assertThat(statement.responsibleName()).isEqualTo(responsible.getName());
        assertThat(statement.referenceMonth()).isEqualTo(referenceMonth);
        assertThat(statement.totalAmountDue()).isEqualTo(new BigDecimal("650.50")); // 500.00 + 150.50
        assertThat(statement.overallDueDate()).isEqualTo(invoice2.getDueDate()); // A data de vencimento mais próxima (inv-002)
        assertThat(statement.items()).hasSize(2);

        // Verifica detalhes de um item (opcional, mas bom para garantir o mapeamento)
        StatementLineItem item1 = statement.items().stream().filter(i -> i.invoiceId().equals("inv-001")).findFirst().orElse(null);
        assertThat(item1).isNotNull();
        assertThat(item1.studentName()).isEqualTo("Pedro Silva");
        assertThat(item1.description()).contains("Mensalidade Turma A"); // Verifica parte da descrição
        assertThat(item1.amount()).isEqualTo(new BigDecimal("500.00"));
        assertThat(item1.dueDate()).isEqualTo(invoice1.getDueDate());

        // Verifica se os métodos dos mocks foram chamados como esperado
        verify(responsibleRepository).findById(responsibleId);
        verify(invoiceRepository).findPendingInvoicesByResponsibleAndMonth(responsibleId, referenceMonth, expectedStatuses);
        verifyNoMoreInteractions(responsibleRepository, invoiceRepository); // Garante que não houve outras interações
    }

    @Test
    @DisplayName("Deve retornar Optional vazio quando o responsável não for encontrado")
    void execute_shouldReturnEmpty_whenResponsibleNotFound() {
        // Arrange
        // Configura o mock para retornar vazio ao buscar o responsável
        when(responsibleRepository.findById(responsibleId)).thenReturn(Optional.empty());

        // Act
        Optional<ConsolidatedStatement> result = generateConsolidatedStatementUseCase.execute(responsibleId, referenceMonth);

        // Assert
        assertThat(result).isEmpty(); // Verifica se o Optional está vazio

        // Verifica que apenas o findById do responsável foi chamado
        verify(responsibleRepository).findById(responsibleId);
        verifyNoInteractions(invoiceRepository); // Garante que o repositório de faturas não foi chamado
    }

    @Test
    @DisplayName("Deve retornar Optional vazio quando não houver faturas pendentes para o mês")
    void execute_shouldReturnEmpty_whenNoPendingInvoicesFound() {
        // Arrange
        List<InvoiceStatus> expectedStatuses = List.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);

        // Configura o mock do responsável
        when(responsibleRepository.findById(responsibleId)).thenReturn(Optional.of(responsible));

        // Configura o mock do invoiceRepository para retornar uma lista vazia
        when(invoiceRepository.findPendingInvoicesByResponsibleAndMonth(responsibleId, referenceMonth, expectedStatuses))
                .thenReturn(Collections.emptyList());

        // Act
        Optional<ConsolidatedStatement> result = generateConsolidatedStatementUseCase.execute(responsibleId, referenceMonth);

        // Assert
        assertThat(result).isEmpty();

        // Verifica as chamadas aos mocks
        verify(responsibleRepository).findById(responsibleId);
        verify(invoiceRepository).findPendingInvoicesByResponsibleAndMonth(responsibleId, referenceMonth, expectedStatuses);
        verifyNoMoreInteractions(responsibleRepository, invoiceRepository);
    }

    @Test
    @DisplayName("Deve calcular corretamente o total e a data de vencimento geral")
    void execute_shouldCalculateTotalsCorrectly() {
        // Arrange - Usa os dados do setUp()
        List<Invoice> pendingInvoices = List.of(invoice1, invoice2);
        List<InvoiceStatus> expectedStatuses = List.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);
        when(responsibleRepository.findById(responsibleId)).thenReturn(Optional.of(responsible));
        when(invoiceRepository.findPendingInvoicesByResponsibleAndMonth(responsibleId, referenceMonth, expectedStatuses))
                .thenReturn(pendingInvoices);

        // Act
        Optional<ConsolidatedStatement> result = generateConsolidatedStatementUseCase.execute(responsibleId, referenceMonth);

        // Assert
        assertThat(result).isPresent();
        ConsolidatedStatement statement = result.get();

        // Verifica especificamente os cálculos
        assertThat(statement.totalAmountDue()).isEqualByComparingTo("650.50"); // Comparação segura para BigDecimal
        assertThat(statement.overallDueDate()).isEqualTo(LocalDate.of(2024, 8, 5)); // Data de vencimento de invoice2
    }

    @Test
    @DisplayName("Deve ignorar faturas com status diferente de PENDING ou OVERDUE")
    void execute_shouldIgnoreInvoicesWithWrongStatus() {
        // Arrange
        Invoice paidInvoice = Invoice.builder()
                .id("inv-paid")
                .enrollment(enrollment1)
                .description("Mensalidade Paga")
                .amount(new BigDecimal("300.00"))
                .status(InvoiceStatus.PAID)
                .referenceMonth(referenceMonth)
                .build();

        when(responsibleRepository.findById(responsibleId)).thenReturn(Optional.of(responsible));
        when(invoiceRepository.findPendingInvoicesByResponsibleAndMonth(responsibleId, referenceMonth,
                List.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE)))
                .thenReturn(List.of(invoice1, paidInvoice));

        // Act
        Optional<ConsolidatedStatement> result = generateConsolidatedStatementUseCase.execute(responsibleId, referenceMonth);

        // Assert
        assertThat(result).isPresent();
        ConsolidatedStatement statement = result.get();
        assertThat(statement.items()).hasSize(1); // Deve ter apenas a fatura PENDING
        assertThat(statement.totalAmountDue()).isEqualTo(new BigDecimal("500.00")); // Apenas o valor da fatura PENDING
    }

    @Test
    @DisplayName("Deve lançar exceção quando enrollment ou student estiverem nulos")
    void execute_shouldThrowException_whenInvalidInvoiceData() {
        // Arrange
        Invoice invalidInvoice = Invoice.builder()
                .id("inv-invalid")
                .enrollment(null) // Enrollment nulo
                .description("Fatura inválida")
                .amount(new BigDecimal("100.00"))
                .status(InvoiceStatus.PENDING)
                .referenceMonth(referenceMonth)
                .build();

        when(responsibleRepository.findById(responsibleId)).thenReturn(Optional.of(responsible));
        when(invoiceRepository.findPendingInvoicesByResponsibleAndMonth(responsibleId, referenceMonth,
                List.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE)))
                .thenReturn(List.of(invoice1, invalidInvoice));

        // Act & Assert
        assertThatThrownBy(() -> generateConsolidatedStatementUseCase.execute(responsibleId, referenceMonth))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fatura inválida");
    }
}
