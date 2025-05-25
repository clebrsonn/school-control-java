package br.com.hyteck.school_control.models.payments;

public enum Types {

    MATRICULA,
    MENSALIDADE,
    DESCONTO, // For itemized discounts (negative amount in InvoiceItem)
    TAXA,     // For general itemized fees or additional charges

}
