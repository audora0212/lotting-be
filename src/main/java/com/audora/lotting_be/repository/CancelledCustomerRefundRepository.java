// src/main/java/com/audora/lotting_be/repository/CancelledCustomerRefundRepository.java
package com.audora.lotting_be.repository;

import com.audora.lotting_be.model.refund.CancelledCustomerRefund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CancelledCustomerRefundRepository extends JpaRepository<CancelledCustomerRefund, Long> {
    // 이미 해당 고객의 해지환불 기록이 있는지 확인하기 위한 메서드
    boolean existsByCustomerId(Integer customerId);
}
