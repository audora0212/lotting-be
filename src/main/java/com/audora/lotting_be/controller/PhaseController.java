package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.payload.request.PhaseModificationRequest;
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
     * Phase 업데이트 엔드포인트 (전체 필드 업데이트)
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

        // 변경된 Phase를 바탕으로 고객의 상태(Status) 업데이트
        Customer customer = updatedPhase.getCustomer();
        customerService.updateStatusFields(customer);
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

    /**
     * @param customerId  고객 id
     * @param phaseNumber 수정할 phase 번호 (예: 1, 2, …)
     * @param request     수정할 필드들을 담은 요청 DTO
     * @return 수정된 Phase 객체
     */
    @PutMapping("/customer/{customerId}/phase/{phaseNumber}/modify")
    public ResponseEntity<Phase> modifyPhaseByCustomerAndPhaseNumber(@PathVariable Integer customerId,
                                                                     @PathVariable Integer phaseNumber,
                                                                     @RequestBody PhaseModificationRequest request) {
        // 1. 고객 조회
        Customer customer = customerService.getCustomerById(customerId);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        // 2. 해당 고객의 phase 목록 중 phaseNumber에 해당하는 phase 찾기
        Phase phase = null;
        if (customer.getPhases() != null) {
            phase = customer.getPhases().stream()
                    .filter(p -> p.getPhaseNumber() != null && p.getPhaseNumber().equals(phaseNumber))
                    .findFirst().orElse(null);
        }
        if (phase == null) {
            return ResponseEntity.notFound().build();
        }
        // 3. 수정 허용 필드 업데이트
        phase.setCharge(request.getCharge());
        phase.setService(request.getService());
        phase.setDiscount(request.getDiscount());
        phase.setExemption(request.getExemption());
        phase.setMove(request.getMove());

        // 4. 전체 재계산 실행하여 납입금액, 대출/자납 관련 금액 등을 다시 계산
        customerService.recalculateEverything(customer);

        // 5. 수정된 phase 반환
        return ResponseEntity.ok(phase);
    }
}
