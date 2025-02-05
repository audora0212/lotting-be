// src/main/java/com/audora/lotting_be/controller/DepositHistoryController.java
package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.DepositHistory;
import com.audora.lotting_be.service.DepositHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deposit-histories")
public class DepositHistoryController {

    @Autowired
    private DepositHistoryService depositHistoryService;

    @PostMapping
    public ResponseEntity<DepositHistory> createDepositHistory(@RequestBody DepositHistory depositHistory) {
        DepositHistory saved = depositHistoryService.createDepositHistory(depositHistory);
        return ResponseEntity.ok(saved);
    }
}
