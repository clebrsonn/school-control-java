package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    // Usar @EntityGraph para definir o plano de fetch
    @EntityGraph(attributePaths = {
            "responsible",                 // Para buscar o responsável da Invoice (usado no GenerateConsolidatedStatementUseCase)
            "items",                       // Para buscar a coleção de InvoiceItems
            "items.enrollment",            // Para buscar o Enrollment de cada InvoiceItem
            "items.enrollment.student",    // Para buscar o Student de cada Enrollment
            "items.enrollment.classroom"   // Para buscar a Classroom de cada Enrollment
    })
    // A query JPQL agora foca apenas nas condições de filtro
    @Query("SELECT inv FROM Invoice inv " +
            "WHERE inv.responsible.id = :responsibleId " +
            "AND inv.referenceMonth = :referenceMonth " +
            "AND inv.status IN :statuses")
    List<Invoice> findPendingInvoicesByResponsibleAndMonth(
            @Param("responsibleId") String responsibleId,
            @Param("referenceMonth") YearMonth referenceMonth,
            @Param("statuses") Collection<InvoiceStatus> statuses
    );

    // Usar @EntityGraph para definir o plano de fetch
    @EntityGraph(attributePaths = {
            "responsible",                 // Para buscar o responsável da Invoice (usado no GenerateConsolidatedStatementUseCase)
            "items",                       // Para buscar a coleção de InvoiceItems
            "items.enrollment",            // Para buscar o Enrollment de cada InvoiceItem
            "items.enrollment.student",    // Para buscar o Student de cada Enrollment
            "items.enrollment.classroom"   // Para buscar a Classroom de cada Enrollment
    })
    // A query JPQL agora foca apenas nas condições de filtro
    @Query("SELECT inv FROM Invoice inv " +
            "WHERE inv.referenceMonth = :referenceMonth " +
            "AND inv.status IN :statuses")
    List<Invoice> findPendingInvoicesByMonth(
            @Param("referenceMonth") YearMonth referenceMonth,
            @Param("statuses") Collection<InvoiceStatus> statuses
    );


    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate dueDate);

    @Query("SELECT CASE WHEN COUNT(inv) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Invoice inv JOIN inv.items item " +
            "WHERE inv.responsible.id = :responsibleId " +
            "AND inv.referenceMonth = :referenceMonth " +
            "AND item.enrollment.id = :enrollmentId")
    boolean existsByResponsibleIdAndReferenceMonthAndItems_Enrollment_Id(
            @Param("responsibleId") String responsibleId,
            @Param("referenceMonth") YearMonth referenceMonth,
            @Param("enrollmentId") String enrollmentId
    );

    long countByStatus(InvoiceStatus status);
}
