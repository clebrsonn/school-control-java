package br.com.hyteck.school_control.services.financial;

import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.LedgerEntry;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.Payment;
import br.com.hyteck.school_control.repositories.financial.LedgerEntryRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service class for managing {@link LedgerEntry} records and posting financial transactions.
 * Ensures that all transactions are balanced (debits equal credits) and that account balances are updated accordingly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountService accountService; // To update account balances

    /**
     * Posts a financial transaction to the ledger. This involves creating a pair of
     * {@link LedgerEntry} records: one debit and one credit.
     * The balances of the affected accounts are also updated.
     * This operation is transactional.
     *
     * @param invoice         Optional {@link Invoice} associated with this transaction.
     * @param payment         Optional {@link Payment} associated with this transaction.
     * @param accountToDebit  The {@link Account} to be debited.
     * @param accountToCredit The {@link Account} to be credited.
     * @param amount          The amount of the transaction. Must be positive.
     * @param transactionDate The date and time of the transaction.
     * @param description     A description of the transaction.
     * @param type            The {@link LedgerEntryType} categorizing the transaction.
     * @throws IllegalArgumentException if accounts are null, amount is not positive, or other validations fail.
     * @throws IllegalStateException    if the debit and credit accounts are the same.
     */
    @Transactional
    public void postTransaction(
            final Invoice invoice, // Can be null
            final Payment payment, // Can be null
            @NotNull final Account accountToDebit,
            @NotNull final Account accountToCredit,
            @NotNull final BigDecimal amount,
            @NotNull final LocalDateTime transactionDate,
            @NotNull final String description,
            @NotNull final LedgerEntryType type) {

        if (accountToDebit == null || accountToCredit == null) {
            throw new IllegalArgumentException("Debit and Credit accounts must not be null.");
        }
        if (accountToDebit.getId().equals(accountToCredit.getId())) {
            throw new IllegalStateException("Debit and Credit accounts cannot be the same.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive.");
        }
        if (transactionDate == null) {
            throw new IllegalArgumentException("Transaction date must not be null.");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction description must not be blank.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Ledger entry type must not be null.");
        }

        log.info("Posting transaction: Type='{}', Amount='{}', Desc='{}'", type, amount, description);
        log.info("DEBIT Account: ID='{}', Name='{}'", accountToDebit.getId(), accountToDebit.getName());
        log.info("CREDIT Account: ID='{}', Name='{}'", accountToCredit.getId(), accountToCredit.getName());

        // Create Debit Entry
        LedgerEntry debitEntry = LedgerEntry.builder()
                .account(accountToDebit)
                .invoice(invoice)
                .payment(payment)
                .debitAmount(amount)
                .creditAmount(BigDecimal.ZERO) // Ensure credit is zero for debit entry
                .transactionDate(transactionDate)
                .description(description)
                .type(type)
                .build();
        ledgerEntryRepository.save(debitEntry);
        log.debug("Saved debit entry ID: {}", debitEntry.getId());

        // Create Credit Entry
        LedgerEntry creditEntry = LedgerEntry.builder()
                .account(accountToCredit)
                .invoice(invoice)
                .payment(payment)
                .debitAmount(BigDecimal.ZERO) // Ensure debit is zero for credit entry
                .creditAmount(amount)
                .transactionDate(transactionDate)
                .description(description)
                .type(type)
                .build();
        ledgerEntryRepository.save(creditEntry);
        log.debug("Saved credit entry ID: {}", creditEntry.getId());

        // Update balances of the affected accounts
        // The AccountService.updateAccountBalance recalculates from all ledger entries for that account.
        accountService.updateAccountBalance(accountToDebit.getId());
        log.info("Updated balance for debit account ID: {}", accountToDebit.getId());

        accountService.updateAccountBalance(accountToCredit.getId());
        log.info("Updated balance for credit account ID: {}", accountToCredit.getId());

        log.info("Transaction posted successfully. Debit Entry ID: {}, Credit Entry ID: {}", debitEntry.getId(), creditEntry.getId());
    }
}
