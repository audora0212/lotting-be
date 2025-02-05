// src/main/java/com/audora/lotting_be/repository/DepositHistoryRepository.java
package com.audora.lotting_be.repository;

import com.audora.lotting_be.model.customer.DepositHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositHistoryRepository extends JpaRepository<DepositHistory, Long> {
}
