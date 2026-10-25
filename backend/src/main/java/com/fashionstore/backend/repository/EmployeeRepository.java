package com.fashionstore.backend.repository;

import com.fashionstore.backend.entity.Employee;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String>, JpaSpecificationExecutor<Employee> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Employee> findAllByEmployeeIdStartingWith(String prefix);

    List<Employee> findAllByBranchId(String branchId);
}
