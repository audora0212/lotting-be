package com.audora.lotting_be.repository;

import com.audora.lotting_be.model.customer.Phase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PhaseRepository extends JpaRepository<Phase, Long> {
    List<Phase> findByCustomerId(Integer customerId);
}
