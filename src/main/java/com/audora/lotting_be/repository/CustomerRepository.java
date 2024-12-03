// CustomerRepository.java
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

}
