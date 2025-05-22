package br.com.hyteck.school_control.models.financial;

/**
 * Defines the type of account in the ledger system.
 * Account types are crucial for classifying accounts in financial statements
 * (e.g., Balance Sheet, Income Statement).
 */
public enum AccountType {
    /**
     * Represents what the company owns (e.g., cash, accounts receivable, equipment).
     * Typically has a debit balance.
     */
    ASSET,

    /**
     * Represents what the company owes to others (e.g., accounts payable, loans).
     * Typically has a credit balance.
     */
    LIABILITY,

    /**
     * Represents the owners' stake in the company (e.g., common stock, retained earnings).
     * Typically has a credit balance.
     */
    EQUITY,

    /**
     * Represents income earned from business activities (e.g., tuition fees, service revenue).
     * Typically has a credit balance. Increases equity.
     */
    REVENUE,

    /**
     * Represents costs incurred to generate revenue (e.g., salaries, rent, discount expenses).
     * Typically has a debit balance. Decreases equity.
     */
    EXPENSE
}
