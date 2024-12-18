// src/main/java/com/audora/lotting_be/repository/CustomerRepository.java
package com.audora.lotting_be.repository;

import com.audora.lotting_be.model.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    @Query(value = "SELECT COALESCE(MAX(id), 0) + 1 FROM customer", nativeQuery = true)
    Integer getNextId();

    List<Customer> findByCustomerDataNameContaining(String name);

    List<Customer> findByCustomerDataNameAndId(String name, Integer id);

    // 정계약한(customertype = 'c') 고객의 수를 세는 쿼리
    long countByCustomertype(String customertype);

    /**
     * ID가 특정 부분 문자열을 포함하는 고객을 찾습니다.
     *
     * 주의: 사용하는 데이터베이스에 따라 CAST 함수의 문법을 조정해야 합니다.
     * 예를 들어, PostgreSQL에서는 CAST(id AS TEXT)를 사용합니다.
     */
    @Query(value = "SELECT * FROM customer WHERE CAST(id AS CHAR) LIKE %:idPart%", nativeQuery = true)
    List<Customer> findByIdContaining(@Param("idPart") String idPart);

    @Query(value = "SELECT * FROM customer WHERE name LIKE CONCAT('%', :name, '%') AND CAST(id AS CHAR) LIKE CONCAT('%', :idPart, '%')", nativeQuery = true)
    List<Customer> findByNameContainingAndIdContaining(@Param("name") String name, @Param("idPart") String idPart);

}
