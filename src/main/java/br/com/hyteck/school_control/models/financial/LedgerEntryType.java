package br.com.hyteck.school_control.models.financial;

/**
 * Defines the type of transaction or event that a LedgerEntry represents.
 * This helps in categorizing and understanding the nature of financial postings.
 */
public enum LedgerEntryType {
    /**
     * Represents a charge for tuition fees.
     * Typically, a debit to Accounts Receivable and a credit to Tuition Revenue.
     */
    TUITION_FEE,

    /**
     * Represents the application of a discount to an invoice.
     * Typically, a debit to Discount Expense and a credit to Accounts Receivable.
     */
    DISCOUNT_APPLIED,

    /**
     * Represents the receipt of a payment from a responsible party.
     * Typically, a debit to a Cash/Bank account and a credit to Accounts Receivable.
     */
    PAYMENT_RECEIVED,

    /**
     * Represents a penalty assessed for an overdue invoice.
     * Typically, a debit to Accounts Receivable and a credit to Penalty Revenue.
     */
    PENALTY_ASSESSED,

    /**
     * Represents a refund issued to a responsible party.
     * Typically, a debit to Accounts Receivable (or specific expense/revenue account) and a credit to Cash/Bank.
     */
    REFUND_ISSUED,

    /**
     * Represents a general journal entry for miscellaneous transactions, adjustments,
     * or corrections that don't fit other specific types.
     */
    GENERAL_JOURNAL,

    /**
     * Represents an initial setup or opening balance for an account.
     * This is often used when migrating data or starting a new accounting period.
     */
    OPENING_BALANCE,

    /**
     * Represents the initial charge for an enrollment fee.
     * Typically, a debit to Accounts Receivable and a credit to Enrollment Fee Revenue.
     */
    ENROLLMENT_FEE_CHARGED,

    /**
     * Represents a year-end closing entry, typically to close out revenue and expense accounts
     * to retained earnings or another equity account.
     */
    CLOSING_ENTRY
}
