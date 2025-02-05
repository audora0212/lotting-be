// src/main/java/com/audora/lotting_be/controller/DepositListController.java

package com.audora.lotting_be.controller;

import com.audora.lotting_be.payload.response.CustomerDepositDTO;
import com.audora.lotting_be.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/depositlist")
public class DepositListController {

    private final CustomerService customerService;

    @Autowired
    public DepositListController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * 모든 회원의 입금 기록 DTO 리스트 반환
     */
    @GetMapping
    public ResponseEntity<List<CustomerDepositDTO>> getAllDepositHistory() {
        List<CustomerDepositDTO> depositDTOList = customerService.getAllCustomerDepositDTOs();
        return ResponseEntity.ok(depositDTOList);
    }
}
