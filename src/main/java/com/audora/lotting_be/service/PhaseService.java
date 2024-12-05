package com.audora.lotting_be.service;

import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.repository.PhaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PhaseService {

    @Autowired
    private PhaseRepository phaseRepository;

    // 특정 고객 ID로 Phase 리스트를 조회하는 메서드
    public List<Phase> getPhasesByCustomerId(Integer customerId) {
        return phaseRepository.findByCustomerId(customerId);
    }

    public Optional<Phase> getPhaseById(Long id) {
        return phaseRepository.findById(id);
    }

    public Phase savePhase(Phase phase) {
        return phaseRepository.save(phase);
    }
}
