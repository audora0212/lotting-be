// FeeRepository.java
package com.audora.lotting_be.repository;

import com.audora.lotting_be.model.Fee.Fee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeRepository extends JpaRepository<Fee, Long> {
    Fee findByGroupnameAndBatch(String groupname, String batch);
}
