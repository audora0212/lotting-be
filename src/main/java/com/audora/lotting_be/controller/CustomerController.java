// CustomerController.java
package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.service.CustomerService;
import com.audora.lotting_be.service.PhaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/customers")
@CrossOrigin(origins = "http://localhost:3000")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PhaseService phaseService;

    @GetMapping("/nextId")
    public ResponseEntity<Integer> getNextCustomerId() {
        Integer nextId = customerService.getNextCustomerId();
        return ResponseEntity.ok(nextId);
    }

    // 고객 생성 엔드포인트
    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        Customer createdCustomer = customerService.createCustomer(customer);
        return ResponseEntity.ok(createdCustomer);
    }

    // 고객 조회 엔드포인트
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Integer id) {
        Customer customer = customerService.getCustomerById(id);
        if (customer != null) {
            return ResponseEntity.ok(customer);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Phase 조회 엔드포인트
    @GetMapping("/{id}/phases")
    public ResponseEntity<List<Phase>> getPhasesByCustomerId(@PathVariable Integer id) {
        List<Phase> phases = phaseService.getPhasesByCustomerId(id);
        if (!phases.isEmpty()) {
            return ResponseEntity.ok(phases);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
