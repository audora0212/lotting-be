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


    @GetMapping
    public List<LateFeeInfo> getLateFees(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String number) {
        return customerService.getLateFeeInfos(name, number);
    }
}
