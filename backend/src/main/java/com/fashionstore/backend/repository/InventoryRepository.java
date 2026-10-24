package com.fashionstore.backend.repository;

import com.fashionstore.backend.entity.Inventory;
import com.fashionstore.backend.entity.InventoryId;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, InventoryId> {

    Page<Inventory> findAllByBranchId(String branchId, Pageable pageable);

    @Query("select i from Inventory i where i.branchId = :branchId and i.quantity <= i.minimumStock")
    Page<Inventory> findLowStock(@Param("branchId") String branchId, Pageable pageable);

    // SERIALIZABLE at the caller also protects the key range when the row does not exist yet.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.branchId = :branchId and i.productId = :productId")
    Optional<Inventory> findForUpdate(
            @Param("branchId") String branchId, @Param("productId") String productId);
}
