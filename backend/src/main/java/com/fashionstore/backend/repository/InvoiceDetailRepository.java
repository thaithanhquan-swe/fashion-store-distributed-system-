package com.fashionstore.backend.repository;

import com.fashionstore.backend.entity.InvoiceDetail;
import com.fashionstore.backend.entity.InvoiceDetailId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceDetailRepository extends JpaRepository<InvoiceDetail, InvoiceDetailId> {
    List<InvoiceDetail> findAllByInvoiceIdOrderByProductId(String invoiceId);

    List<InvoiceDetail> findAllByInvoiceIdIn(Collection<String> invoiceIds);
}
