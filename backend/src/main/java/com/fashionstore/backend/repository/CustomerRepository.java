package com.fashionstore.backend.repository;

import com.fashionstore.backend.entity.Customer;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String>, JpaSpecificationExecutor<Customer> {

    boolean existsByPhoneNumber(String phoneNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Customer> findAllByCustomerIdStartingWith(String prefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Customer> findForUpdateByCustomerId(String customerId);
}
