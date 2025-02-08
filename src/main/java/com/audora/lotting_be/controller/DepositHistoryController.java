package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.DepositHistory;
import com.audora.lotting_be.service.DepositHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deposit") // 입금내역 관련 엔드포인트
public class DepositHistoryController {

    @Autowired
    private DepositHistoryService depositHistoryService;

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
