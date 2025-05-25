# Event Documentation

This document provides details on the various application events used within the School Control system.

## 1. InvoiceCreatedEvent

*   **Purpose:** Signifies that a new invoice has been successfully created and persisted in the system. This event is crucial for initiating downstream processes like ledger entry creation for the new invoice charges.
*   **Typical Data:**
    *   `source`: The component that published the event (usually the use case or service that created the invoice).
    *   `invoice`: The `Invoice` object that was created. This includes details like invoice ID, responsible party, items, amount, due date, etc.
*   **Listeners:**
    *   `LedgerEntryCreationListener`: Listens to this event to create the initial debit (Accounts Receivable) and credit (e.g., Tuition Revenue, Enrollment Fee Revenue) ledger entries corresponding to the invoice.
    *   `InvoiceNotificationListener` (Presumed): Could listen to this event to send a notification (e.g., email) to the responsible party about the new invoice.

## 2. PaymentProcessedEvent

*   **Purpose:** Indicates that a payment attempt has been processed for an invoice. This event triggers ledger updates to reflect the payment and potentially updates the invoice status.
*   **Typical Data:**
    *   `source`: The component that published the event (typically `ProcessPaymentUseCase`).
    *   `paymentId`: The ID of the `Payment` record.
    *   `invoiceId`: The ID of the `Invoice` to which the payment applies.
    *   `amountPaid`: The `BigDecimal` amount of the payment.
    *   `paymentStatus`: The `PaymentStatus` of the payment record (e.g., COMPLETED, FAILED).
    *   `invoiceStatus`: The `InvoiceStatus` of the invoice *after* this payment attempt (e.g., PAID, PENDING, OVERDUE). This status is determined by the `Invoice` aggregate itself.
    *   `responsibleId`: The ID of the `Responsible` party associated with the invoice.
*   **Listeners:**
    *   `PaymentLedgerListener`: Listens to this event to create ledger entries reflecting the payment (e.g., debiting a cash/bank account and crediting Accounts Receivable for the responsible party). Implements idempotency to prevent duplicate ledger entries for the same payment.
    *   `PaymentNotificationListener` (Presumed): Could listen to this event to send a payment confirmation to the responsible party.
    *   `InvoiceStatusChangeNotificationListener` (Presumed, or handled by a more general status change listener): If the invoice status changes as a result of the payment, this event could trigger notifications.

## 3. PenaltyAssessedEvent

*   **Purpose:** Fired when a penalty (e.g., for late payment) is assessed on an invoice. This event is used to update the financial ledger.
*   **Typical Data:**
    *   `source`: The component that published the event (e.g., a use case for assessing penalties).
    *   `invoiceId`: The ID of the `Invoice` on which the penalty is assessed.
    *   `penaltyAmount`: The `BigDecimal` amount of the penalty.
    *   `responsibleId`: The ID of the `Responsible` party for the invoice.
    *   `assessmentDate`: `LocalDate` when the penalty was assessed.
*   **Listeners:**
    *   `PenaltyLedgerListener`: Listens to this event to create ledger entries for the penalty (e.g., debiting Accounts Receivable for the responsible party and crediting a Penalty Revenue account). Implements idempotency based on invoice ID and event type.
    *   `PenaltyNotificationListener` (Presumed): Could inform the responsible party about the assessed penalty.

## 4. BatchInvoiceGeneratedEvent (Conceptual)

*   **Purpose:** (Assuming this event exists or is planned based on context) Signifies that a batch of invoices (e.g., monthly tuition fees for all active students) has been generated.
*   **Typical Data:**
    *   `source`: The component that published the event (e.g., a batch invoice generation service).
    *   `generatedInvoiceIds`: A list of IDs of the newly generated invoices.
    *   `referenceMonth`: The `YearMonth` for which the batch was generated.
    *   `count`: Number of invoices generated.
*   **Listeners:**
    *   Could trigger a summary notification to administrators.
    *   Individual `InvoiceCreatedEvent` might be published for each invoice within the batch, which would then be handled by `LedgerEntryCreationListener` and potentially `InvoiceNotificationListener`. Alternatively, a dedicated batch listener could handle initial notifications or summary logging.

## 5. VerificationTokenCreatedEvent

*   **Purpose:** Fired when a new verification token (e.g., for email verification upon user registration) is created and associated with a user.
*   **Typical Data:**
    *   `source`: The component that published the event (e.g., user registration service).
    *   `verificationToken`: The `VerificationToken` object, which includes the token string and a reference to the `User` it's for.
*   **Listeners:**
    *   `VerificationEmailListener`: Listens to this event to send an asynchronous email to the user containing the verification link (using the token).

## 6. StudentEnrolledEvent

*   **Purpose:** Indicates that a student has been successfully enrolled in a classroom. This can trigger subsequent processes like generating enrollment fee invoices or notifications.
*   **Typical Data:**
    *   `source`: The component that published the event (typically `CreateEnrollment` use case).
    *   `enrollmentId`: The ID of the newly created `Enrollment` record.
    *   `studentId`: The ID of the `Student`.
    *   `classroomId`: The ID of the `ClassRoom`.
    *   `enrollmentDate`: The `LocalDate` of the enrollment.
*   **Listeners:**
    *   (Currently, `CreateEnrollment` use case directly creates the enrollment fee invoice if applicable. This event could decouple that in the future.)
    *   A hypothetical `EnrollmentNotificationListener` could inform parents or administrative staff.
    *   Could trigger setup of student access to learning materials, etc.

## 7. InvoiceStatusChangedEvent (Presumed)

*   **Purpose:** (Presumed based on the need to react to invoice status changes not directly tied to a payment, e.g., manual cancellation, or status changes due to corrections/adjustments). This event would provide a generic way to handle invoice status updates.
*   **Typical Data:**
    *   `source`: The component that changed the invoice status.
    *   `invoiceId`: The ID of the `Invoice` whose status changed.
    *   `oldStatus`: The previous `InvoiceStatus`.
    *   `newStatus`: The new `InvoiceStatus`.
    *   `reason`: (Optional) A string explaining the reason for the status change.
*   **Listeners:**
    *   `InvoiceStatusChangeNotificationListener`: Could send notifications based on specific status transitions (e.g., invoice overdue, invoice cancelled).
    *   Could trigger other business logic, like suspending services if an invoice becomes long overdue.

This documentation aims to help developers understand the flow of events and their impact within the system. It should be updated as new events are added or existing ones are modified.
