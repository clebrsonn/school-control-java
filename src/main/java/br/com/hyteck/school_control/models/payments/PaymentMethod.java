package br.com.hyteck.school_control.models.payments;

public enum PaymentMethod {
    PIX("PIX"),
    BOLETO(""),
    CREDIT_CARD("CREDITO"),
    DEBIT_CARD("DEBITO"),
    BANK_TRANSFER("TRANSFERENCIA_BANCARIA");

    PaymentMethod(String type) {
    }
}
