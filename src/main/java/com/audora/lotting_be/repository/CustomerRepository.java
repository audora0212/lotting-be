// CustomerRepository.java
package com.audora.lotting_be.repository;

import com.audora.lotting_be.model.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
}
