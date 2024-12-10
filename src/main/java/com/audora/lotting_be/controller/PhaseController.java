// PhaseController.java
package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.service.CustomerService;
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
    @Autowired
    private CustomerService customerService;

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

        // 변경된 Phase를 바탕으로 Status 재계산
        Customer customer = updatedPhase.getCustomer();
        // Status 필드를 업데이트하는 메서드 (CustomerService에 존재한다고 가정)
        customerService.updateStatusFields(customer);
        // 업데이트된 상태를 DB에 반영
        customerService.saveCustomer(customer);

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
