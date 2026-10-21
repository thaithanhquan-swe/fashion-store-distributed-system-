package com.fashionstore.backend.repository;

import com.fashionstore.backend.entity.Branch;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {

    Optional<Branch> findFirstByActiveTrueOrderByBranchIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Branch> findAllByBranchIdStartingWith(String prefix);
}
