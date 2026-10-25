package com.fashionstore.backend.repository;

import com.fashionstore.backend.entity.Invoice;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {
    Page<Invoice> findByCustomerId(String customerId, Pageable pageable);

    Page<Invoice> findAllByBranchId(String branchId, Pageable pageable);

    List<Invoice> findAllByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Invoice> findAllByBranchIdAndCreatedAtBetween(String branchId, LocalDateTime start, LocalDateTime end);

    List<Invoice> findAllByEmployeeIdAndCreatedAtBetween(String employeeId, LocalDateTime start, LocalDateTime end);

    List<Invoice> findTop5ByEmployeeIdOrderByCreatedAtDesc(String employeeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.invoiceId = :invoiceId")
    Optional<Invoice> findForUpdate(@Param("invoiceId") String invoiceId);
}
