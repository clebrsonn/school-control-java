package br.com.hyteck.school_control.services.financial;

import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.financial.LedgerEntry;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.Payment;
import br.com.hyteck.school_control.repositories.financial.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private LedgerService ledgerService;

    private Account debitAccount;
    private Account creditAccount;
    private Invoice invoice;
    private Payment payment;
    private LocalDateTime transactionDate;

    @BeforeEach
    void setUp() {
        debitAccount = Account.builder().id("debitAccId").name("Debit Account").type(AccountType.ASSET).build();
        creditAccount = Account.builder().id("creditAccId").name("Credit Account").type(AccountType.REVENUE).build();
        invoice = Invoice.builder().id("invoiceId1").build();
        payment = Payment.builder().id("paymentId1").build();
        transactionDate = LocalDateTime.now();
    }

    @Test
    void postTransaction_ShouldSaveDebitAndCreditEntries_AndUpdateAccountBalances() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        String description = "Test transaction";
        LedgerEntryType type = LedgerEntryType.GENERAL_JOURNAL;

        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(accountService).updateAccountBalance(anyString());

        // Act
        ledgerService.postTransaction(invoice, payment, debitAccount, creditAccount, amount, transactionDate, description, type);

        // Assert
        ArgumentCaptor<LedgerEntry> ledgerEntryCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(2)).save(ledgerEntryCaptor.capture());
        verify(accountService).updateAccountBalance(debitAccount.getId());
        verify(accountService).updateAccountBalance(creditAccount.getId());

        List<LedgerEntry> capturedEntries = ledgerEntryCaptor.getAllValues();
        LedgerEntry savedDebitEntry = capturedEntries.stream().filter(e -> e.getDebitAmount().compareTo(BigDecimal.ZERO) > 0).findFirst().orElse(null);
        LedgerEntry savedCreditEntry = capturedEntries.stream().filter(e -> e.getCreditAmount().compareTo(BigDecimal.ZERO) > 0).findFirst().orElse(null);

        assertNotNull(savedDebitEntry);
        assertEquals(debitAccount, savedDebitEntry.getAccount());
        assertEquals(amount, savedDebitEntry.getDebitAmount());
        assertEquals(BigDecimal.ZERO, savedDebitEntry.getCreditAmount());
        assertEquals(invoice, savedDebitEntry.getInvoice());
        assertEquals(payment, savedDebitEntry.getPayment());
        assertEquals(description, savedDebitEntry.getDescription());
        assertEquals(type, savedDebitEntry.getType());
        assertEquals(transactionDate, savedDebitEntry.getTransactionDate());


        assertNotNull(savedCreditEntry);
        assertEquals(creditAccount, savedCreditEntry.getAccount());
        assertEquals(amount, savedCreditEntry.getCreditAmount());
        assertEquals(BigDecimal.ZERO, savedCreditEntry.getDebitAmount());
        assertEquals(invoice, savedCreditEntry.getInvoice());
        assertEquals(payment, savedCreditEntry.getPayment());
        assertEquals(description, savedCreditEntry.getDescription());
        assertEquals(type, savedCreditEntry.getType());
        assertEquals(transactionDate, savedCreditEntry.getTransactionDate());
    }
    
    @Test
    void postTransaction_ShouldHandleNullInvoiceAndPayment() {
        BigDecimal amount = new BigDecimal("50.00");
        String description = "General expense";
        LedgerEntryType type = LedgerEntryType.GENERAL_JOURNAL;

        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(accountService).updateAccountBalance(anyString());
        
        ledgerService.postTransaction(null, null, debitAccount, creditAccount, amount, transactionDate, description, type);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(2)).save(captor.capture());
        List<LedgerEntry> entries = captor.getAllValues();
        assertTrue(entries.stream().allMatch(e -> e.getInvoice() == null && e.getPayment() == null));
    }


    @Test
    void postTransaction_ShouldThrowException_WhenDebitAccountIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ledgerService.postTransaction(invoice, payment, null, creditAccount, BigDecimal.TEN, transactionDate, "Desc", LedgerEntryType.GENERAL_JOURNAL));
        assertEquals("Debit and Credit accounts must not be null.", ex.getMessage());
    }

    @Test
    void postTransaction_ShouldThrowException_WhenCreditAccountIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ledgerService.postTransaction(invoice, payment, debitAccount, null, BigDecimal.TEN, transactionDate, "Desc", LedgerEntryType.GENERAL_JOURNAL));
        assertEquals("Debit and Credit accounts must not be null.", ex.getMessage());
    }

    @Test
    void postTransaction_ShouldThrowException_WhenDebitAndCreditAccountsAreSame() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                ledgerService.postTransaction(invoice, payment, debitAccount, debitAccount, BigDecimal.TEN, transactionDate, "Desc", LedgerEntryType.GENERAL_JOURNAL));
        assertEquals("Debit and Credit accounts cannot be the same.", ex.getMessage());
    }

    @Test
    void postTransaction_ShouldThrowException_WhenAmountIsZero() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ledgerService.postTransaction(invoice, payment, debitAccount, creditAccount, BigDecimal.ZERO, transactionDate, "Desc", LedgerEntryType.GENERAL_JOURNAL));
        assertEquals("Transaction amount must be positive.", ex.getMessage());
    }
    
    @Test
    void postTransaction_ShouldThrowException_WhenAmountIsNegative() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ledgerService.postTransaction(invoice, payment, debitAccount, creditAccount, new BigDecimal("-10"), transactionDate, "Desc", LedgerEntryType.GENERAL_JOURNAL));
        assertEquals("Transaction amount must be positive.", ex.getMessage());
    }

    @Test
    void postTransaction_ShouldThrowException_WhenTransactionDateIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ledgerService.postTransaction(invoice, payment, debitAccount, creditAccount, BigDecimal.TEN, null, "Desc", LedgerEntryType.GENERAL_JOURNAL));
        assertEquals("Transaction date must not be null.", ex.getMessage());
    }

    @Test
    void postTransaction_ShouldThrowException_WhenDescriptionIsBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ledgerService.postTransaction(invoice, payment, debitAccount, creditAccount, BigDecimal.TEN, transactionDate, "  ", LedgerEntryType.GENERAL_JOURNAL));
        assertEquals("Transaction description must not be blank.", ex.getMessage());
    }
    
    @Test
    void postTransaction_ShouldThrowException_WhenDescriptionIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ledgerService.postTransaction(invoice, payment, debitAccount, creditAccount, BigDecimal.TEN, transactionDate, null, LedgerEntryType.GENERAL_JOURNAL));
        assertEquals("Transaction description must not be blank.", ex.getMessage());
    }

    @Test
    void postTransaction_ShouldThrowException_WhenEntryTypeIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ledgerService.postTransaction(invoice, payment, debitAccount, creditAccount, BigDecimal.TEN, transactionDate, "Desc", null));
        assertEquals("Ledger entry type must not be null.", ex.getMessage());
    }
}
