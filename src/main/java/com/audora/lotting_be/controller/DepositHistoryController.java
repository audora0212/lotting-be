package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.DepositHistory;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.repository.DepositHistoryRepository;
import com.audora.lotting_be.service.CustomerService;
import com.audora.lotting_be.service.DepositHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

// 기존 @RestController, @RequestMapping("/deposit") 그대로 유지
@RestController
@RequestMapping("/deposit") // 입금내역 관련 엔드포인트
public class DepositHistoryController {

    @Autowired
    private DepositHistoryService depositHistoryService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    // [GET] 특정 고객의 입금내역 조회
    // URL 예: GET /deposit/customer/123
    @GetMapping("/customer/{userId}")
    public ResponseEntity<List<DepositHistory>> getDepositHistoriesByCustomerId(@PathVariable Integer userId) {
        Customer customer = customerService.getCustomerById(userId);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        List<DepositHistory> depositHistories = customer.getDepositHistories();
        return ResponseEntity.ok(depositHistories);
    }

    // [GET] 단일 입금내역 조회
    @GetMapping("/{id}")
    public ResponseEntity<DepositHistory> getDepositHistoryById(@PathVariable Long id) {
        Optional<DepositHistory> depositHistoryOpt = depositHistoryRepository.findById(id);
        return depositHistoryOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // [POST] 입금내역 생성
    @PostMapping
    public ResponseEntity<DepositHistory> createDepositHistory(@RequestBody DepositHistory depositHistory) {
        DepositHistory saved = depositHistoryService.createDepositHistory(depositHistory);
        return ResponseEntity.ok(saved);
    }

    // [PUT] 입금내역 수정
    @PutMapping("/{id}")
    public ResponseEntity<DepositHistory> updateDepositHistory(
            @PathVariable Long id,
            @RequestBody DepositHistory updatedDepositHistory) {
        DepositHistory updated = depositHistoryService.updateDepositHistory(id, updatedDepositHistory);
        return ResponseEntity.ok(updated);
    }

    // [DELETE] 입금내역 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepositHistory(@PathVariable Long id) {
        depositHistoryService.deleteDepositHistory(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────
    // 새로운 엔드포인트: 1차 ~ 10차의 총 입금액(charged)과 미납액(sum) 리턴
    // ─────────────────────────────────────────────────────
    @GetMapping("/phase-summary")
    public ResponseEntity<List<PhaseSummaryDTO>> getPhaseSummaries() {
        // 모든 고객과 그에 연결된 phase들을 조회 (고객에 대한 재계산은 이미 수행되었다고 가정)
        List<Customer> customers = customerService.getAllCustomersWithPhases();

        // 1차부터 10차까지 각 phase의 합계를 저장할 DTO를 초기화
        Map<Integer, PhaseSummaryDTO> summaryMap = new HashMap<>();
        for (int phase = 1; phase <= 10; phase++) {
            summaryMap.put(phase, new PhaseSummaryDTO(phase, 0L, 0L));
        }

        // 모든 고객의 phase 정보를 순회하여 각 phase의 charged와 sum을 누적
        for (Customer customer : customers) {
            if (customer.getPhases() != null) {
                for (Phase phase : customer.getPhases()) {
                    Integer phaseNo = phase.getPhaseNumber();
                    if (phaseNo != null && phaseNo >= 1 && phaseNo <= 10) {
                        PhaseSummaryDTO dto = summaryMap.get(phaseNo);
                        dto.setTotalDeposited(dto.getTotalDeposited() + (phase.getCharged() != null ? phase.getCharged() : 0L));
                        dto.setTotalUnpaid(dto.getTotalUnpaid() + (phase.getSum() != null ? phase.getSum() : 0L));
                    }
                }
            }
        }

        // 결과를 phase 번호 순서대로 리스트로 변환
        List<PhaseSummaryDTO> summaries = new ArrayList<>(summaryMap.values());
        summaries.sort(Comparator.comparingInt(PhaseSummaryDTO::getPhaseNumber));
        return ResponseEntity.ok(summaries);
    }

    // DTO 클래스: 각 phase의 번호, 총 입금액, 미납액
    public static class PhaseSummaryDTO {
        private int phaseNumber;
        private Long totalDeposited;
        private Long totalUnpaid;

        public PhaseSummaryDTO() {
        }

        public PhaseSummaryDTO(int phaseNumber, Long totalDeposited, Long totalUnpaid) {
            this.phaseNumber = phaseNumber;
            this.totalDeposited = totalDeposited;
            this.totalUnpaid = totalUnpaid;
        }

        public int getPhaseNumber() {
            return phaseNumber;
        }

        public void setPhaseNumber(int phaseNumber) {
            this.phaseNumber = phaseNumber;
        }

        public Long getTotalDeposited() {
            return totalDeposited;
        }

        public void setTotalDeposited(Long totalDeposited) {
            this.totalDeposited = totalDeposited;
        }

        public Long getTotalUnpaid() {
            return totalUnpaid;
        }

        public void setTotalUnpaid(Long totalUnpaid) {
            this.totalUnpaid = totalUnpaid;
        }
    }
}
