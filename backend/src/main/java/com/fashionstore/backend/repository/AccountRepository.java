package com.fashionstore.backend.repository;

import com.fashionstore.backend.entity.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

    boolean existsByUsername(String username);

    boolean existsByEmployeeId(String employeeId);

    Optional<Account> findByUsername(String username);
}
