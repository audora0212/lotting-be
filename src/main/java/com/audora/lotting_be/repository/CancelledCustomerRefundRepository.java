// src/main/java/com/audora/lotting_be/repository/CancelledCustomerRefundRepository.java
package com.audora.lotting_be.repository;

import com.audora.lotting_be.model.refund.CancelledCustomerRefund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CancelledCustomerRefundRepository extends JpaRepository<CancelledCustomerRefund, Long> {
    boolean existsByCustomerId(Integer customerId);
}
