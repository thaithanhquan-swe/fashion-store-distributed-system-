package com.fashionstore.backend.repository;

import com.fashionstore.backend.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {

    @Query(value = "SELECT COALESCE(MAX(TRY_CONVERT(INT, SUBSTRING(MaChiNhanh, 3, 8))), 0) "
            + "FROM dbo.ChiNhanh WITH (TABLOCKX, HOLDLOCK) "
            + "WHERE MaChiNhanh LIKE 'CN%'", nativeQuery = true)
    Integer findHighestGeneratedBranchNumber();
}
