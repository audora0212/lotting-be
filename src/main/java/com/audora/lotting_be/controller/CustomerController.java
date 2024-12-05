// CustomerController.java
package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.Loan;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.payload.response.MessageResponse;
import com.audora.lotting_be.service.CustomerService;
import com.audora.lotting_be.service.PhaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/customers")
@CrossOrigin(origins = "http://localhost:3000")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PhaseService phaseService;

    // 고객 생성시 아이디 받아오기
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

    // 고객 검색 페이지
    @GetMapping("/search")
    public ResponseEntity<List<Customer>> searchCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String number) {

        List<Customer> customers = customerService.searchCustomers(name, number);
        return ResponseEntity.ok(customers);
    }

    // 고객 삭제 엔드포인트
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Integer id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    //납입금 관리 페이지 미납차수
    @GetMapping("/{customerId}/pending-phases")
    public ResponseEntity<List<Phase>> getPendingPhases(@PathVariable Integer customerId) {
        List<Phase> pendingPhases = customerService.getPendingPhases(customerId);
        if (pendingPhases != null) {
            return ResponseEntity.ok(pendingPhases);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    //납입금 관리 페이지 완납차수
    @GetMapping("/{customerId}/completed-phases")
    public ResponseEntity<List<Phase>> getCompletedPhases(@PathVariable Integer customerId) {
        List<Phase> completedPhases = customerService.getCompletedPhases(customerId);
        if (completedPhases != null) {
            return ResponseEntity.ok(completedPhases);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/loan")
    public ResponseEntity<Loan> getLoanByCustomerId(@PathVariable Integer id) {
        Optional<Customer> optionalCustomer = Optional.ofNullable(customerService.getCustomerById(id));
        if (!optionalCustomer.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Loan loan = optionalCustomer.get().getLoan();
        return ResponseEntity.ok(loan);
    }

    @PutMapping("/{id}/loan")
    public ResponseEntity<Customer> updateLoanByCustomerId(@PathVariable Integer id, @RequestBody Loan updatedLoan) {
        Optional<Customer> optionalCustomer = Optional.ofNullable(customerService.getCustomerById(id));
        if (!optionalCustomer.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Customer customer = optionalCustomer.get();
        Loan loan = customer.getLoan();

        // Loan 필드 업데이트
        loan.setLoandate(updatedLoan.getLoandate());
        loan.setLoanammount(updatedLoan.getLoanammount());
        loan.setSelfdate(updatedLoan.getSelfdate());
        loan.setSelfammount(updatedLoan.getSelfammount());
        loan.setLoanselfsum(updatedLoan.getLoanselfsum());
        loan.setLoanselfcurrent(updatedLoan.getLoanselfcurrent());

        customer.setLoan(loan);
        customerService.saveCustomer(customer);

        return ResponseEntity.ok(customer);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelCustomer(@PathVariable Integer id) {
        Customer customer = customerService.getCustomerById(id);
        if (customer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Error: Customer not found."));
        }

        customer.setCustomertype("x");
        customerService.saveCustomer(customer);

        return ResponseEntity.ok(new MessageResponse("Customer cancelled successfully."));
    }

    // 고객 수정 엔드포인트
    // 고객 수정 엔드포인트
    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Integer id, @RequestBody Customer updatedCustomer) {
        Customer existingCustomer = customerService.getCustomerById(id);
        if (existingCustomer == null) {
            return ResponseEntity.notFound().build();
        }

        // 기존 고객 정보에 업데이트된 정보 반영
        existingCustomer.setCustomertype(updatedCustomer.getCustomertype());
        existingCustomer.setType(updatedCustomer.getType());
        existingCustomer.setGroupname(updatedCustomer.getGroupname());
        existingCustomer.setTurn(updatedCustomer.getTurn());
        existingCustomer.setBatch(updatedCustomer.getBatch());
        existingCustomer.setRegisterdate(updatedCustomer.getRegisterdate());
        existingCustomer.setRegisterprice(updatedCustomer.getRegisterprice());
        existingCustomer.setAdditional(updatedCustomer.getAdditional());
        existingCustomer.setSpecialnote(updatedCustomer.getSpecialnote());
        existingCustomer.setPrizewinning(updatedCustomer.getPrizewinning());

        // 임베디드 객체 업데이트
        existingCustomer.setCustomerData(updatedCustomer.getCustomerData());
        existingCustomer.setFinancial(updatedCustomer.getFinancial());
        existingCustomer.setLegalAddress(updatedCustomer.getLegalAddress());
        existingCustomer.setPostreceive(updatedCustomer.getPostreceive());
        existingCustomer.setDeposits(updatedCustomer.getDeposits());
        existingCustomer.setAttachments(updatedCustomer.getAttachments());
        existingCustomer.setLoan(updatedCustomer.getLoan());
        existingCustomer.setResponsible(updatedCustomer.getResponsible());
        existingCustomer.setDahim(updatedCustomer.getDahim());
        existingCustomer.setMgm(updatedCustomer.getMgm());
        existingCustomer.setFirstemp(updatedCustomer.getFirstemp());
        existingCustomer.setSecondemp(updatedCustomer.getSecondemp());
        existingCustomer.setMeetingattend(updatedCustomer.getMeetingattend());
        existingCustomer.setVotemachine(updatedCustomer.getVotemachine());

        // Phases 및 Status 등 필요한 추가 업데이트 로직

        Customer savedCustomer = customerService.saveCustomer(existingCustomer);
        return ResponseEntity.ok(savedCustomer);
    }
}
