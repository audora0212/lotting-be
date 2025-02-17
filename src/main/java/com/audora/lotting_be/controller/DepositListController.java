package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.DepositHistory;
import com.audora.lotting_be.service.DepositHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/depositlist") // 입금 여부 한눈에 보는 페이지 전용
public class DepositListController {

    private final DepositHistoryService depositHistoryService;

    @Autowired
    public DepositListController(DepositHistoryService depositHistoryService) {
        this.depositHistoryService = depositHistoryService;
    }

    /**
     * 모든 DepositHistory를 반환
     */
    @GetMapping
    public ResponseEntity<List<DepositHistory>> getAllDepositHistory() {
        List<DepositHistory> depositHistories = depositHistoryService.getAllDepositHistories();
        return ResponseEntity.ok(depositHistories);
    }
}
