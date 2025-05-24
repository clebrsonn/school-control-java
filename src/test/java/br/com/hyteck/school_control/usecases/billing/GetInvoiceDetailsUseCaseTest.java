package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.classrooms.Classroom;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.finance.Account;
import br.com.hyteck.school_control.models.finance.AccountType;
import br.com.hyteck.school_control.models.finance.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.*;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.LedgerEntryRepository;
import br.com.hyteck.school_control.repositories.PaymentRepository;
import br.com.hyteck.school_control.services.AccountService;
import br.com.hyteck.school_control.web.dtos.billing.InvoiceDetailDto;
import br.com.hyteck.school_control.web.dtos.billing.InvoiceItemDetailDto;
import br.com.hyteck.school_control.web.dtos.payments.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GetInvoiceDetailsUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private GetInvoiceDetailsUseCase getInvoiceDetailsUseCase;

    private Responsible responsible1;
    private Account arAccountResp1;
    private Student student1;
    private Classroom classroom1;
    private Enrollment enrollment1;

    @BeforeEach
    void setUp() {
        responsible1 = Responsible.builder().id("resp1").name("Responsible One").build();
        arAccountResp1 = Account.builder().id("ar1").type(AccountType.ASSET).responsible(responsible1).name("A/R - Responsible One").build();
        student1 = Student.builder().id("stud1").name("Student One").responsible(responsible1).build();
        classroom1 = Classroom.builder().id("classA").name("Class A").build();
        enrollment1 = Enrollment.builder()
            .id("enroll1").student(student1).classroom(classroom1)
            .monthlyFee(new BigDecimal("300.00")).status(Enrollment.Status.ACTIVE).build();
    }

    private Invoice createMockInvoice(String id, Responsible responsible, BigDecimal netOriginalAmount, List<InvoiceItem> items) {
        Invoice invoice = Invoice.builder()
                .id(id)
                .responsible(responsible)
                .referenceMonth(YearMonth.of(2023, 11))
                .status(InvoiceStatus.PENDING)
                .originalAmount(netOriginalAmount) // This should be the NET amount
                .dueDate(LocalDate.of(2023, 11, 10))
                .issueDate(LocalDate.of(2023, 11, 1))
                .items(new ArrayList<>()) // Initialize
                .build();
        items.forEach(item -> { // Associate items with invoice
            item.setInvoice(invoice);
            invoice.getItems().add(item);
        });
        return invoice;
    }
    
    private InvoiceItem createTuitionItem(String id, Enrollment enrollment, BigDecimal amount) {
        return InvoiceItem.builder().id(id).enrollment(enrollment).amount(amount)
            .description("Tuition Fee").type(Types.MENSALIDADE).build();
    }

    private InvoiceItem createDiscountItem(String id, String description, BigDecimal discountAmount) {
        return InvoiceItem.builder().id(id).description(description).amount(discountAmount.negate()) // Negative
            .type(Types.DESCONTO).enrollment(null).build();
    }


    @Test
    void execute_invoiceNotFound_shouldReturnEmptyOptional() {
        when(invoiceRepository.findById("nonExistentInv")).thenReturn(Optional.empty());
        Optional<InvoiceDetailDto> result = getInvoiceDetailsUseCase.execute("nonExistentInv");
        assertTrue(result.isEmpty());
        verify(ledgerEntryRepository, never()).getBalanceForInvoiceOnAccount(any(), any());
    }

    @Test
    void execute_invoiceFound_withItemizedDiscount_shouldReturnCorrectDto() {
        // Setup Invoice with itemized discount
        InvoiceItem tuitionItem = createTuitionItem("itemT", enrollment1, new BigDecimal("300.00"));
        InvoiceItem discountItem = createDiscountItem("itemD", "Sibling Discount", new BigDecimal("30.00"));
        BigDecimal netOriginalAmount = new BigDecimal("270.00"); // 300 - 30
        Invoice invoiceWithDiscount = createMockInvoice("invWithDisc", responsible1, netOriginalAmount, List.of(tuitionItem, discountItem));
        
        Payment payment1 = Payment.builder()
            .id("pay1").invoice(invoiceWithDiscount).amountPaid(new BigDecimal("100.00"))
            .paymentDate(LocalDateTime.now().minusDays(2)).paymentMethod(PaymentMethod.CREDIT_CARD).build();
        List<Payment> payments = List.of(payment1);

        when(invoiceRepository.findById("invWithDisc")).thenReturn(Optional.of(invoiceWithDiscount));
        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenReturn(arAccountResp1);

        // Mock ledger sums for ad-hoc adjustments (should be zero if only itemized discount)
        when(ledgerEntryRepository.sumCreditAmountByInvoiceIdAndAccountIdAndType(
                "invWithDisc", arAccountResp1.getId(), LedgerEntryType.DISCOUNT_APPLIED))
                .thenReturn(BigDecimal.ZERO); // No ad-hoc ledger discounts in this scenario
        when(ledgerEntryRepository.sumDebitAmountByInvoiceIdAndAccountIdAndType(
                "invWithDisc", arAccountResp1.getId(), LedgerEntryType.PENALTY_ASSESSED))
                .thenReturn(new BigDecimal("10.00")); // Example ad-hoc penalty
        when(ledgerEntryRepository.sumCreditAmountByInvoiceIdAndAccountIdAndType(
                "invWithDisc", arAccountResp1.getId(), LedgerEntryType.PAYMENT_RECEIVED))
                .thenReturn(new BigDecimal("100.00"));

        // Net Original: 270. Ad-hoc Discount: 0. Penalty: +10. Payment: -100.
        // Expected Balance: 270 + 10 - 100 = 180
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), "invWithDisc"))
                .thenReturn(new BigDecimal("180.00")); 

        when(paymentRepository.findByInvoiceId("invWithDisc")).thenReturn(payments);

        // Act
        Optional<InvoiceDetailDto> resultOpt = getInvoiceDetailsUseCase.execute("invWithDisc");

        // Assert
        assertTrue(resultOpt.isPresent());
        InvoiceDetailDto result = resultOpt.get();

        assertEquals("invWithDisc", result.getId());
        assertEquals(netOriginalAmount, result.getOriginalAmount(), "DTO OriginalAmount should be NET");
        
        assertEquals(BigDecimal.ZERO, result.getTotalAdHocDiscountsApplied(), "DTO TotalAdHocDiscounts should be zero");
        assertEquals(new BigDecimal("10.00"), result.getTotalPenaltiesAssessed());
        assertEquals(new BigDecimal("100.00"), result.getTotalPaymentsReceived());
        assertEquals(new BigDecimal("180.00"), result.getCurrentBalanceDue());

        assertEquals(2, result.getItems().size(), "Should include tuition and discount items");
        assertTrue(result.getItems().stream().anyMatch(item -> item.type().equals(Types.MENSALIDADE.name()) && item.amount().compareTo(new BigDecimal("300.00")) == 0));
        assertTrue(result.getItems().stream().anyMatch(item -> item.type().equals(Types.DESCONTO.name()) && item.amount().compareTo(new BigDecimal("-30.00")) == 0));
        
        assertEquals(1, result.getPayments().size());
    }
    
    @Test
    void execute_invoiceFound_withAdHocLedgerDiscount_shouldReflectInDto() {
        // Setup Invoice (originalAmount is net, but we also have an ad-hoc ledger discount)
        InvoiceItem tuitionItem = createTuitionItem("itemT2", enrollment1, new BigDecimal("500.00"));
        // No itemized discount in this invoice's items list, so netOriginalAmount = gross
        Invoice invoiceAdHoc = createMockInvoice("invAdHoc", responsible1, new BigDecimal("500.00"), List.of(tuitionItem));
        
        when(invoiceRepository.findById("invAdHoc")).thenReturn(Optional.of(invoiceAdHoc));
        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenReturn(arAccountResp1);

        // Mock ledger sums
        when(ledgerEntryRepository.sumCreditAmountByInvoiceIdAndAccountIdAndType(
                "invAdHoc", arAccountResp1.getId(), LedgerEntryType.DISCOUNT_APPLIED))
                .thenReturn(new BigDecimal("25.00")); // Ad-hoc ledger discount
        when(ledgerEntryRepository.sumDebitAmountByInvoiceIdAndAccountIdAndType(
                "invAdHoc", arAccountResp1.getId(), LedgerEntryType.PENALTY_ASSESSED))
                .thenReturn(BigDecimal.ZERO); 
        when(ledgerEntryRepository.sumCreditAmountByInvoiceIdAndAccountIdAndType(
                "invAdHoc", arAccountResp1.getId(), LedgerEntryType.PAYMENT_RECEIVED))
                .thenReturn(BigDecimal.ZERO);

        // Net Original: 500. Ad-hoc Discount: -25. Penalty: 0. Payment: 0.
        // Expected Balance: 500 - 25 = 475
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), "invAdHoc"))
                .thenReturn(new BigDecimal("475.00")); 

        when(paymentRepository.findByInvoiceId("invAdHoc")).thenReturn(Collections.emptyList());

        // Act
        Optional<InvoiceDetailDto> resultOpt = getInvoiceDetailsUseCase.execute("invAdHoc");

        // Assert
        assertTrue(resultOpt.isPresent());
        InvoiceDetailDto result = resultOpt.get();

        assertEquals("invAdHoc", result.getId());
        assertEquals(new BigDecimal("500.00"), result.getOriginalAmount(), "DTO OriginalAmount is net (here, same as gross)");
        assertEquals(new BigDecimal("25.00"), result.getTotalAdHocDiscountsApplied(), "DTO TotalAdHocDiscounts should reflect ledger discount");
        assertEquals(BigDecimal.ZERO, result.getTotalPenaltiesAssessed());
        assertEquals(BigDecimal.ZERO, result.getTotalPaymentsReceived());
        assertEquals(new BigDecimal("475.00"), result.getCurrentBalanceDue());
        assertEquals(1, result.getItems().size());
    }

    @Test
    void execute_invoiceWithNoResponsible_shouldReturnEmpty() {
        Invoice inv = createMockInvoice("invNoResp", null, BigDecimal.ZERO, Collections.emptyList());
        inv.setResponsible(null); 
        when(invoiceRepository.findById("invNoResp")).thenReturn(Optional.of(inv));
        Optional<InvoiceDetailDto> result = getInvoiceDetailsUseCase.execute("invNoResp");
        assertTrue(result.isEmpty());
        verify(accountService, never()).findOrCreateResponsibleARAccount(any());
    }

    @Test
    void execute_errorFetchingARAccount_shouldReturnEmpty() {
        Invoice inv = createMockInvoice("invErrAR", responsible1, new BigDecimal("100"), Collections.emptyList());
        when(invoiceRepository.findById("invErrAR")).thenReturn(Optional.of(inv));
        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenThrow(new RuntimeException("DB error"));
        Optional<InvoiceDetailDto> result = getInvoiceDetailsUseCase.execute("invErrAR");
        assertTrue(result.isEmpty());
    }
}
