// PhaseController.java
package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.service.PhaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/phases")
public class PhaseController {

    @Autowired
    private PhaseService phaseService;

    /**
     * Phase 업데이트 엔드포인트
     * PUT /phases/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Phase> updatePhase(@PathVariable Long id, @RequestBody Phase phaseDetails) {
        Optional<Phase> optionalPhase = phaseService.getPhaseById(id);
        if (!optionalPhase.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Phase phase = optionalPhase.get();
        phase.setPlanneddate(phaseDetails.getPlanneddate());
        phase.setFullpaiddate(phaseDetails.getFullpaiddate());
        phase.setCharge(phaseDetails.getCharge());
        phase.setDiscount(phaseDetails.getDiscount());
        phase.setExemption(phaseDetails.getExemption());
        phase.setService(phaseDetails.getService());
        phase.setMove(phaseDetails.getMove());
        phase.setFeesum(phaseDetails.getFeesum());
        phase.setCharged(phaseDetails.getCharged());
        phase.setSum(phaseDetails.getSum());

        Phase updatedPhase = phaseService.savePhase(phase);
        return ResponseEntity.ok(updatedPhase);
    }

    /**
     * Phase 조회 엔드포인트
     * GET /phases/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Phase> getPhaseById(@PathVariable Long id) {
        Optional<Phase> optionalPhase = phaseService.getPhaseById(id);
        return optionalPhase.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
