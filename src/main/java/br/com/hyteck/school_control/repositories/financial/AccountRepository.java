package br.com.hyteck.school_control.repositories.financial;

import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.payments.Responsible;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA Repository for {@link Account} entities.
 * Provides standard CRUD operations and custom query methods for accessing account data.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    /**
     * Finds an account by its name.
     * Account names should ideally be unique, at least within certain contexts
     * (e.g., a specific responsible's A/R account, or a general revenue account).
     *
     * @param name The name of the account to find.
     * @return An {@link Optional} containing the {@link Account} if found, or empty if not.
     */
    Optional<Account> findByName(String name);

    /**
     * Finds an account by its type and associated responsible party.
     * This is particularly useful for finding specific accounts like an Accounts Receivable (A/R)
     * account for a given responsible person.
     *
     * @param type        The {@link AccountType} of the account (e.g., ASSET for A/R).
     * @param responsible The {@link Responsible} party associated with the account.
     * @return An {@link Optional} containing the {@link Account} if found, or empty if not.
     */
    Optional<Account> findByTypeAndResponsible(AccountType type, Responsible responsible);

    /**
     * Finds an account by its type and name.
     * Useful for finding general ledger accounts that are not tied to a specific responsible,
     * such as "Tuition Revenue" or "Discount Expenses".
     *
     * @param type The {@link AccountType} of the account.
     * @param name The name of the account.
     * @return An {@link Optional} containing the {@link Account} if found, or empty if not.
     */
    Optional<Account> findByTypeAndName(AccountType type, String name);

    /**
     * Finds an account by its type, associated responsible party, and name.
     * This is useful for uniquely identifying accounts that are specific to a responsible party
     * but also have a distinct name (e.g. if a responsible could have multiple accounts of the same type
     * but with different names/purposes).
     *
     * @param type        The {@link AccountType} of the account.
     * @param responsible The {@link Responsible} party associated with the account.
     * @param name        The name of the account.
     * @return An {@link Optional} containing the {@link Account} if found, or empty if not.
     */
    Optional<Account> findByTypeAndResponsibleAndName(AccountType type, Responsible responsible, String name);
}
