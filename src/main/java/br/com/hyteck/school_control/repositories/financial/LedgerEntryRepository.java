package br.com.hyteck.school_control.repositories.financial;

import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.LedgerEntry;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * JPA Repository for {@link LedgerEntry} entities.
 * Provides standard CRUD operations and custom query methods for accessing ledger entry data.
 */
@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, String> {

    /**
     * Finds all ledger entries associated with a specific account.
     *
     * @param account The {@link Account} for which to retrieve ledger entries.
     * @return A list of {@link LedgerEntry} records.
     */
    List<LedgerEntry> findByAccount(Account account);

    /**
     * Finds all ledger entries associated with a specific invoice.
     * This can be used to reconstruct the financial history or impact of a single invoice.
     *
     * @param invoice The {@link Invoice} for which to retrieve ledger entries.
     * @return A list of {@link LedgerEntry} records related to the given invoice.
     */
    List<LedgerEntry> findByInvoice(Invoice invoice);
    
    /**
     * Calculates the sum of debit amounts for a given account.
     *
     * @param accountId The ID of the account.
     * @return The total debit amount, or BigDecimal.ZERO if no debit entries exist.
     */
    @Query("SELECT COALESCE(SUM(le.debitAmount), 0) FROM LedgerEntry le WHERE le.account.id = :accountId")
    BigDecimal sumDebitAmountByAccountId(@Param("accountId") String accountId);

    /**
     * Calculates the sum of credit amounts for a given account.
     *
     * @param accountId The ID of the account.
     * @return The total credit amount, or BigDecimal.ZERO if no credit entries exist.
     */
    @Query("SELECT COALESCE(SUM(le.creditAmount), 0) FROM LedgerEntry le WHERE le.account.id = :accountId")
    BigDecimal sumCreditAmountByAccountId(@Param("accountId") String accountId);

    /**
     * Calculates the balance of ledger entries associated with a specific invoice for a particular account.
     * This is useful for determining how much of an invoice has been "cleared" against a specific account
     * (e.g., how much of an Accounts Receivable for an invoice has been paid off).
     * The balance is typically calculated as (total debits - total credits) for A/R accounts.
     *
     * @param accountId The ID of the account (e.g., the responsible's A/R account).
     * @param invoiceId The ID of the invoice.
     * @return The net balance (debits - credits) for the given invoice on the specified account.
     */
    @Query("SELECT COALESCE(SUM(le.debitAmount), 0) - COALESCE(SUM(le.creditAmount), 0) " +
           "FROM LedgerEntry le " +
           "WHERE le.account.id = :accountId AND le.invoice.id = :invoiceId")
    BigDecimal getBalanceForInvoiceOnAccount(@Param("accountId") String accountId, @Param("invoiceId") String invoiceId);

    /**
     * Calculates the sum of debit amounts for a specific invoice on a particular account, filtered by entry type.
     *
     * @param invoiceId The ID of the invoice.
     * @param accountId The ID of the account.
     * @param entryType The type of ledger entry (e.g., PENALTY_ASSESSED).
     * @return The total sum of debit amounts, or BigDecimal.ZERO if none found.
     */
    @Query("SELECT COALESCE(SUM(le.debitAmount), 0) " +
           "FROM LedgerEntry le " +
           "WHERE le.invoice.id = :invoiceId AND le.account.id = :accountId AND le.type = :entryType")
    BigDecimal sumDebitAmountByInvoiceIdAndAccountIdAndType(
            @Param("invoiceId") String invoiceId,
            @Param("accountId") String accountId,
            @Param("entryType") LedgerEntryType entryType
    );

    /**
     * Calculates the sum of credit amounts for a specific invoice on a particular account, filtered by entry type.
     *
     * @param invoiceId The ID of the invoice.
     * @param accountId The ID of the account.
     * @param entryType The type of ledger entry (e.g., DISCOUNT_APPLIED, PAYMENT_RECEIVED).
     * @return The total sum of credit amounts, or BigDecimal.ZERO if none found.
     */
    @Query("SELECT COALESCE(SUM(le.creditAmount), 0) " +
           "FROM LedgerEntry le " +
           "WHERE le.invoice.id = :invoiceId AND le.account.id = :accountId AND le.type = :entryType")
    BigDecimal sumCreditAmountByInvoiceIdAndAccountIdAndType(
            @Param("invoiceId") String invoiceId,
            @Param("accountId") String accountId,
            @Param("entryType") LedgerEntryType entryType
    );
}
