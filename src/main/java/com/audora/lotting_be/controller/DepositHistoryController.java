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

@RestController
@RequestMapping("/deposit")
public class DepositHistoryController {

    @Autowired
    private DepositHistoryService depositHistoryService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    @GetMapping("/customer/{userId}")
    public ResponseEntity<List<DepositHistory>> getDepositHistoriesByCustomerId(@PathVariable Integer userId) {
        Customer customer = customerService.getCustomerById(userId);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        List<DepositHistory> depositHistories = customer.getDepositHistories();
        return ResponseEntity.ok(depositHistories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepositHistory> getDepositHistoryById(@PathVariable Long id) {
        Optional<DepositHistory> depositHistoryOpt = depositHistoryRepository.findById(id);
        return depositHistoryOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DepositHistory> createDepositHistory(@RequestBody DepositHistory depositHistory) {
        DepositHistory saved = depositHistoryService.createDepositHistory(depositHistory);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepositHistory> updateDepositHistory(
            @PathVariable Long id,
            @RequestBody DepositHistory updatedDepositHistory) {
        DepositHistory updated = depositHistoryService.updateDepositHistory(id, updatedDepositHistory);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepositHistory(@PathVariable Long id) {
        depositHistoryService.deleteDepositHistory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/phase-summary")
    public ResponseEntity<List<PhaseSummaryDTO>> getPhaseSummaries() {
        List<Customer> customers = customerService.getAllCustomersWithPhases();

        Map<Integer, PhaseSummaryDTO> summaryMap = new HashMap<>();
        for (int phase = 1; phase <= 10; phase++) {
            summaryMap.put(phase, new PhaseSummaryDTO(phase, 0L, 0L));
        }

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

        List<PhaseSummaryDTO> summaries = new ArrayList<>(summaryMap.values());
        summaries.sort(Comparator.comparingInt(PhaseSummaryDTO::getPhaseNumber));
        return ResponseEntity.ok(summaries);
    }

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
