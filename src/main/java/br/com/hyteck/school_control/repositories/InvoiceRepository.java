package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.Types;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    /**
     * Retrieves all pending invoices for a given responsible and reference month, including all necessary relationships to avoid N+1 queries.
     *
     * @param responsibleId   the ID of the responsible party
     * @param referenceMonth  the reference month (YearMonth)
     * @param statuses        the collection of invoice statuses to filter (e.g., PENDING, OVERDUE)
     * @return a list of invoices matching the criteria, with related entities eagerly fetched
     */
    @EntityGraph(attributePaths = {
            "responsible",
            "items",
            "items.enrollment",
            "items.enrollment.student",
            "items.enrollment.classroom"
    })
    @Query("SELECT inv FROM Invoice inv " +
            "WHERE inv.responsible.id = :responsibleId " +
            "AND inv.referenceMonth = :referenceMonth " +
            "AND inv.status IN :statuses")
    List<Invoice> findPendingInvoicesByResponsibleAndMonth(
            @Param("responsibleId") String responsibleId,
            @Param("referenceMonth") YearMonth referenceMonth,
            @Param("statuses") Collection<InvoiceStatus> statuses
    );

    /**
     * Retrieves all pending invoices for a given reference month, including all necessary relationships to avoid N+1 queries.
     *
     * @param referenceMonth  the reference month (YearMonth)
     * @param statuses        the collection of invoice statuses to filter (e.g., PENDING, OVERDUE)
     * @return a list of invoices matching the criteria, with related entities eagerly fetched
     */
    @EntityGraph(attributePaths = {
            "responsible",
            "items",
            "items.enrollment",
            "items.enrollment.student",
            "items.enrollment.classroom"
    })
    @Query("SELECT inv FROM Invoice inv " +
            "WHERE inv.referenceMonth = :referenceMonth " +
            "AND inv.status IN :statuses")
    List<Invoice> findPendingInvoicesByMonth(
            @Param("referenceMonth") YearMonth referenceMonth,
            @Param("statuses") Collection<InvoiceStatus> statuses
    );

    /**
     * Sums the total amount of invoices for a given month and statuses.
     *
     * @param referenceMonth  the reference month (YearMonth)
     * @param statuses        the collection of invoice statuses to filter (e.g., PENDING, OVERDUE)
     * @return the total sum of invoice amounts
     */
    @Query("SELECT COALESCE(SUM(inv.amount), 0) FROM Invoice inv WHERE inv.referenceMonth = :referenceMonth AND inv.status IN :statuses")
    BigDecimal sumAmountByReferenceMonthAndStatuses(
            @Param("referenceMonth") YearMonth referenceMonth,
            @Param("statuses") Collection<InvoiceStatus> statuses);

    /**
     * Finds all invoices by status and due date before a given date.
     *
     * @param status   the invoice status
     * @param dueDate  the due date threshold
     * @return a list of invoices matching the criteria
     */
    @EntityGraph(attributePaths = {
            "responsible",
            "items",
            "items.enrollment",
            "items.enrollment.student",
            "items.enrollment.classroom"
    })
    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate dueDate);

    /**
     * Checks if an invoice exists for a responsible, month, enrollment, and type.
     *
     * @param responsibleId   the ID of the responsible party
     * @param referenceMonth  the reference month (YearMonth)
     * @param enrollmentId    the enrollment ID
     * @param type            the type of invoice item
     * @return true if such an invoice exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(inv) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Invoice inv JOIN inv.items item " +
            "WHERE inv.responsible.id = :responsibleId " +
            "AND inv.referenceMonth = :referenceMonth " +
            "AND inv.referenceMonth = :referenceMonth " +
            "AND item.enrollment.id = :enrollmentId " +
            "AND item.type = :type")
    boolean existsByResponsibleIdAndReferenceMonthAndItems_Enrollment_Id(
            @Param("responsibleId") String responsibleId,
            @Param("referenceMonth") YearMonth referenceMonth,
            @Param("enrollmentId") String enrollmentId,
            @Param("type") Types type
    );

    /**
     * Counts the number of invoices by status.
     *
     * @param status the invoice status
     * @return the count of invoices with the given status
     */
    long countByStatus(InvoiceStatus status);
}
