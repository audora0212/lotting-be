// FeeController.java
package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.Fee.Fee;
import com.audora.lotting_be.service.FeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/fees")
public class FeeController {

    @Autowired
    private FeeService feeService;

    /**
     * 새로운 Fee 생성
     *
     * @param fee 생성할 Fee 객체
     * @return 생성된 Fee 객체
     */
    @PostMapping
    public ResponseEntity<Fee> createFee(@RequestBody Fee fee) {
        Fee createdFee = feeService.createFee(fee);
        return ResponseEntity.ok(createdFee);
    }

    /**
     * ID로 Fee 조회
     *
     * @param id Fee의 ID
     * @return Fee 객체 (존재할 경우)
     */
    @GetMapping("/{id}")
    public ResponseEntity<Fee> getFeeById(@PathVariable Long id) {
        Fee fee = feeService.getFeeById(id);
        if (fee != null) {
            return ResponseEntity.ok(fee);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 모든 Fee 조회
     *
     * @return 모든 Fee의 리스트
     */
    @GetMapping
    public ResponseEntity<List<Fee>> getAllFees() {
        List<Fee> fees = feeService.getAllFees();
        return ResponseEntity.ok(fees);
    }
}
