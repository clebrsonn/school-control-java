package br.com.hyteck.school_control.services.financial;

import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import br.com.hyteck.school_control.repositories.financial.AccountRepository;
import br.com.hyteck.school_control.repositories.financial.LedgerEntryRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service class for managing financial {@link Account} entities.
 * Provides functionalities to create, find, and manage account balances.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ResponsibleRepository responsibleRepository; // To fetch Responsible if only ID is given

    /**
     * Creates a new financial account if an account with the same name and type (and responsible, if applicable)
     * does not already exist.
     *
     * @param name        The name of the account.
     * @param type        The {@link AccountType} of the account.
     * @param responsible The {@link Responsible} party, if this account is tied to one (e.g., Accounts Receivable).
     *                    Can be null for general accounts.
     * @return The newly created or existing {@link Account}.
     * @throws IllegalArgumentException if required parameters are missing or invalid.
     */
    @Transactional
    public Account findOrCreateAccount(@NotBlank final String name, @NotNull final AccountType type, final Responsible responsible) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be blank.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Account type cannot be null.");
        }

        Optional<Account> existingAccount;
        if (responsible != null) {
            log.debug("Searching for account with name '{}', type '{}', and responsible ID '{}'", name, type, responsible.getId());
            existingAccount = accountRepository.findByTypeAndResponsibleAndName(type, responsible, name);
        } else {
            log.debug("Searching for general account with name '{}' and type '{}'", name, type);
            existingAccount = accountRepository.findByTypeAndName(type, name);
        }

        if (existingAccount.isPresent()) {
            log.info("Account '{}' of type '{}' already exists with ID '{}'. Returning existing.", name, type, existingAccount.get().getId());
            return existingAccount.get();
        } else {
            log.info("Creating new account: Name='{}', Type='{}'{}{}", name, type,
                    (responsible != null ? ", ResponsibleID='" + responsible.getId() + "'" : ""),
                    (responsible != null ? ", ResponsibleName='" + responsible.getName() + "'" : ""));
            Account newAccount = Account.builder()
                    .name(name)
                    .type(type)
                    .responsible(responsible)
                    .balance(BigDecimal.ZERO) // Initial balance is always zero
                    .build();
            return accountRepository.save(newAccount);
        }
    }
    
    /**
     * Finds or creates an Accounts Receivable (A/R) account for a specific responsible party.
     * The account name is standardized as "Accounts Receivable - [Responsible Name]".
     *
     * @param responsibleId The ID of the {@link Responsible} party.
     * @return The existing or newly created A/R {@link Account} for the responsible party.
     * @throws br.com.hyteck.school_control.exceptions.ResourceNotFoundException if the responsible party with the given ID is not found.
     */
    @Transactional
    public Account findOrCreateResponsibleARAccount(@NotNull final String responsibleId) {
        final Responsible responsible = responsibleRepository.findById(responsibleId)
                .orElseThrow(() -> new br.com.hyteck.school_control.exceptions.ResourceNotFoundException(
                        "Responsible not found with ID: " + responsibleId + " while creating A/R account."
                ));

        String arAccountName = "Accounts Receivable - " + responsible.getName();
        // Check if an A/R account for this responsible already exists by a more specific query if available
        // For now, using the general findOrCreateAccount which will check by type, responsible, and name.
        return findOrCreateAccount(arAccountName, AccountType.ASSET, responsible);
    }


    /**
     * Calculates the current balance of an account by summing its debit and credit ledger entries.
     * Balance = SUM(debitAmount) - SUM(creditAmount) for ASSET and EXPENSE accounts.
     * Balance = SUM(creditAmount) - SUM(debitAmount) for LIABILITY, EQUITY, and REVENUE accounts.
     *
     * @param accountId The ID of the account for which to calculate the balance.
     * @return The calculated balance as a {@link BigDecimal}.
     * @throws br.com.hyteck.school_control.exceptions.ResourceNotFoundException if the account with the given ID is not found.
     */
    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(@NotNull final String accountId) {
        final Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new br.com.hyteck.school_control.exceptions.ResourceNotFoundException(
                        "Account not found with ID: " + accountId + " for balance calculation."
                ));

        BigDecimal totalDebits = ledgerEntryRepository.sumDebitAmountByAccountId(accountId);
        BigDecimal totalCredits = ledgerEntryRepository.sumCreditAmountByAccountId(accountId);
        
        totalDebits = totalDebits == null ? BigDecimal.ZERO : totalDebits;
        totalCredits = totalCredits == null ? BigDecimal.ZERO : totalCredits;

        log.debug("Calculating balance for Account ID '{}' ({}): Total Debits = {}, Total Credits = {}",
                accountId, account.getName(), totalDebits, totalCredits);

        // For ASSET and EXPENSE accounts, balance = debits - credits
        // For LIABILITY, EQUITY, REVENUE accounts, balance = credits - debits
        if (account.getType() == AccountType.ASSET || account.getType() == AccountType.EXPENSE) {
            return totalDebits.subtract(totalCredits);
        } else {
            return totalCredits.subtract(totalDebits);
        }
    }

    /**
     * Updates the persisted balance of an account.
     * This method should ideally be called after all related ledger entries for a transaction
     * have been successfully posted and the overall transaction is being committed.
     *
     * @param accountId The ID of the account whose balance needs to be updated.
     */
    @Transactional
    public void updateAccountBalance(@NotNull final String accountId) {
        final Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new br.com.hyteck.school_control.exceptions.ResourceNotFoundException(
                        "Account not found with ID: " + accountId + " for balance update."
                ));
        
        BigDecimal calculatedBalance = getAccountBalance(accountId); // Uses the method above to get the fresh balance
        
        if (!account.getBalance().equals(calculatedBalance)) {
            log.info("Updating persisted balance for Account ID '{}' ({}). Old: {}, New: {}",
                    accountId, account.getName(), account.getBalance(), calculatedBalance);
            account.setBalance(calculatedBalance);
            accountRepository.save(account);
        } else {
            log.debug("Persisted balance for Account ID '{}' ({}) is already up-to-date: {}",
                    accountId, account.getName(), account.getBalance());
        }
    }
}
