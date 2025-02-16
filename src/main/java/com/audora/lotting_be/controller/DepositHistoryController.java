// src/main/java/com/audora/lotting_be/controller/DepositHistoryController.java
package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.DepositHistory;
import com.audora.lotting_be.repository.DepositHistoryRepository;
import com.audora.lotting_be.service.CustomerService;
import com.audora.lotting_be.service.DepositHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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
}
