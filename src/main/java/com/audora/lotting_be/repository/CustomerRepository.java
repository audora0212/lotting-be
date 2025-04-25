// src/main/java/com/audora/lotting_be/repository/CustomerRepository.java
package com.audora.lotting_be.repository;

import com.audora.lotting_be.model.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    @Query(value = "SELECT COALESCE(MAX(id), 0) + 1 FROM customer", nativeQuery = true)
    Integer getNextId();

    List<Customer> findByCustomerDataNameContaining(String name);

    List<Customer> findByCustomerDataNameAndId(String name, Integer id);

    Optional<Customer> findByCustomerDataName(String name);

    // (customertype = 'c') 카운트
    long countByCustomertype(String customertype);


    @Query(value = "SELECT * FROM customer WHERE CAST(id AS CHAR) LIKE %:idPart%", nativeQuery = true)
    List<Customer> findByIdContaining(@Param("idPart") String idPart);

    @Query(value = "SELECT * FROM customer WHERE name LIKE CONCAT('%', :name, '%') AND CAST(id AS CHAR) LIKE CONCAT('%', :idPart, '%')", nativeQuery = true)
    List<Customer> findByNameContainingAndIdContaining(@Param("name") String name, @Param("idPart") String idPart);

}
