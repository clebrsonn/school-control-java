package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.Collection;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    // Query para buscar faturas de um responsável em um mês específico com status específicos
    @Query("SELECT inv FROM Invoice inv " +
            "JOIN FETCH inv.enrollment enr " + // JOIN FETCH para evitar N+1 com enrollment
            "JOIN FETCH enr.student stu " +    // JOIN FETCH para evitar N+1 com student
            "JOIN FETCH enr.classroom cl " +   // JOIN FETCH para classroom (opcional, se precisar do nome)
            "WHERE stu.responsible.id = :responsibleId " +
            "AND inv.referenceMonth = :referenceMonth " +
            "AND inv.status IN :statuses")
    List<Invoice> findPendingInvoicesByResponsibleAndMonth(
            @Param("responsibleId") String responsibleId,
            @Param("referenceMonth") YearMonth referenceMonth,
            @Param("statuses") Collection<InvoiceStatus> statuses
    );

}
