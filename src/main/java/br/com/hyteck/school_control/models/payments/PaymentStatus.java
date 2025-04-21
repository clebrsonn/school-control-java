package br.com.hyteck.school_control.models.payments;


public enum PaymentStatus {
    PENDING,    // Pagamento iniciado, aguardando confirmação
    COMPLETED,  // Pagamento confirmado com sucesso
    FAILED,     // Pagamento falhou
    REFUNDED    // Pagamento estornado
}