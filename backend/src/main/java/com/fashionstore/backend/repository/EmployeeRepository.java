package com.fashionstore.backend.repository;

import com.fashionstore.backend.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    @Query(value = "SELECT COALESCE(MAX(TRY_CONVERT(INT, SUBSTRING(MaNhanVien, 3, 8))), 0) "
            + "FROM dbo.NhanVien WITH (TABLOCKX, HOLDLOCK) "
            + "WHERE MaNhanVien LIKE 'NV%'", nativeQuery = true)
    Integer findHighestGeneratedEmployeeNumber();
}
