// src/main/java/com/audora/lotting_be/controller/LateFeesController.java
package com.audora.lotting_be.controller;

import com.audora.lotting_be.payload.response.LateFeeInfo;
import com.audora.lotting_be.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/latefees")
public class LateFeesController {

    @Autowired
    private CustomerService customerService;

    /**
     * GET /latefees
     * 회원의 연체료 정보를 조회합니다.
     *
     * @param name   (선택 사항) 회원 이름
     * @param number (선택 사항) 회원 번호
     * @return LateFeeInfo 리스트
     */
    @GetMapping
    public List<LateFeeInfo> getLateFees(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String number) {
        return customerService.getLateFeeInfos(name, number);
    }
}
