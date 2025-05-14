package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceItem;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.usecases.notification.CreateNotification;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat; // Para formatar moeda
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter; // Para formatar data
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Log4j2
public class GenerateInvoicesForParents {

    private final EnrollmentRepository enrollmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CreateNotification createNotification; // <<< INJETAR

    private static final Locale BRAZIL_LOCALE = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", BRAZIL_LOCALE);
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(BRAZIL_LOCALE);


    public GenerateInvoicesForParents(EnrollmentRepository enrollmentRepository,
                                      InvoiceRepository invoiceRepository, CreateNotification createNotification) { // <<< ADICIONAR AO CONSTRUTOR
        this.enrollmentRepository = enrollmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.createNotification = createNotification;
    }

    @Transactional
    public void execute(YearMonth targetMonth) {
        log.info("Iniciando geração de faturas mensais para o mês: {}", targetMonth);

        List<Enrollment> activeEnrollments = enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE);
        log.info("Encontradas {} matrículas ativas.", activeEnrollments.size());

        Map<String, Invoice> invoicesByResponsibles = new HashMap<>();

        for (Enrollment enrollment : activeEnrollments) {
            if (enrollment.getMonthlyFee() == null || enrollment.getMonthlyFee().compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Matrícula ID {} (Aluno: {}) não possui valor de mensalidade definido ou é zero. Pulando.",
                        enrollment.getId(), enrollment.getStudent().getName());
                continue;
            }

            Responsible responsible = enrollment.getStudent().getResponsible();
            if (responsible == null) {
                log.warn("Matrícula ID {} (Aluno: {}) não tem um responsável associado ao estudante. Pulando.",
                        enrollment.getId(), enrollment.getStudent().getName());
                continue;
            }

            // Verificar se já existe uma fatura para esta matrícula, responsável e mês de referência
            boolean alreadyBilled = invoiceRepository.existsByResponsibleIdAndReferenceMonthAndItems_Enrollment_Id(
                    responsible.getId(),
                    targetMonth,
                    enrollment.getId()
            );

            if (alreadyBilled) {
                log.info("Mensalidade para matrícula ID {} (Aluno: {}), mês {} já faturada para o responsável {}. Pulando.",
                        enrollment.getId(), enrollment.getStudent().getName(), targetMonth, responsible.getId());
                continue;
            }
            Invoice monthlyInvoice;
            if(!invoicesByResponsibles.containsKey(enrollment.getStudent().getResponsible().getId())){
                LocalDate dueDate= LocalDate.now().getDayOfMonth() > 10 ?LocalDate.of(targetMonth.getYear(), targetMonth.getMonthValue()+1, 10) : targetMonth.atDay(10);
                monthlyInvoice= Invoice.builder()
                        .responsible(responsible)
                        .referenceMonth(targetMonth)
                        .issueDate(LocalDate.now())
                        .dueDate(dueDate) // Exemplo: Vencimento todo dia 10 do mês de referência
                        .status(InvoiceStatus.PENDING)
                        .description("Fatura Mensalidade " + targetMonth.getMonth().getDisplayName(TextStyle.FULL, BRAZIL_LOCALE) + "/" + targetMonth.getYear())
                        .build();

                invoicesByResponsibles.put(enrollment.getStudent().getResponsible().getId(), monthlyInvoice);

            }else{
                monthlyInvoice= invoicesByResponsibles.get(enrollment.getStudent().getResponsible().getId());
            }

            // Criar o InvoiceItem para a mensalidade
            InvoiceItem monthlyFeeItem = InvoiceItem.builder()
                    .enrollment(enrollment)
                    .description("Mensalidade " + targetMonth.getMonth().getDisplayName(TextStyle.FULL, BRAZIL_LOCALE) + "/" + targetMonth.getYear() +
                            " - Aluno: " + enrollment.getStudent().getName())
                    .amount(enrollment.getMonthlyFee())
                    .build();

            monthlyInvoice.addItem(monthlyFeeItem);
            monthlyInvoice.setAmount(monthlyInvoice.calculateTotalAmount());
        }


        List<Invoice> savedInvoices = invoiceRepository.saveAll(invoicesByResponsibles.values()); // Salva e obtém o ID

        // --- DISPARAR NOTIFICAÇÃO ---
        invoicesByResponsibles.forEach((key, value) -> {


            String studentName = value.getItems().getFirst().getEnrollment().getStudent() != null ? value.getItems().getFirst().getEnrollment().getStudent().getName() : "N/D";
            String formattedAmount = CURRENCY_FORMATTER.format(value.getAmount());
            String formattedDueDate = value.getDueDate().format(DATE_FORMATTER);

            String notificationMessage = String.format(
                    "Nova fatura de mensalidade gerada para %s no valor de %s, com vencimento em %s.",
                    studentName,
                    formattedAmount,
                    formattedDueDate
            );
            // O link deve apontar para a visualização da fatura no frontend
            // Exemplo: /invoices/{invoiceId} ou /billing/my-invoices/{invoiceId}
            String notificationLink = "/invoices/" + value.getId(); // Ajuste conforme suas rotas de frontend
            String notificationType = "NEW_MONTHLY_INVOICE";

            try {
                createNotification.execute(
                        key, // ID do usuário do responsável
                        notificationMessage,
                        notificationLink,
                        notificationType
                );
                log.info("Notificação sobre nova fatura enviada para o usuário do responsável ID: {}", key);
            } catch (Exception e) {
                log.error("Falha ao enviar notificação para o usuário do responsável ID {}: {}", key, e.getMessage(), e);
                // Decidir se a falha na notificação deve impedir a transação (geralmente não)
            }
        });
        log.info("Geração de faturas mensais para {} concluída.", targetMonth);
    }
}