// src/main/java/com/audora/lotting_be/repository/CustomerRepository.java
package com.audora.lotting_be.repository;

import com.audora.lotting_be.model.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    @Query(value = "SELECT COALESCE(MAX(id), 0) + 1 FROM customer", nativeQuery = true)
    Integer getNextId();

    List<Customer> findByCustomerDataNameContaining(String name);

    List<Customer> findByCustomerDataNameAndId(String name, Integer id);

    // 정계약한(customertype = 'c') 고객의 수를 세는 쿼리
    long countByCustomertype(String customertype);
}
