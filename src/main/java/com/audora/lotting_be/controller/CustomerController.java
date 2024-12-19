// src/main/java/com/audora/lotting_be/controller/CustomerController.java
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

import java.util.*;

@RestController
@RequestMapping("/customers")
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

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        System.out.println("Received Customer Data: " + customer);
        if (customer.getDeposits() != null) {
            System.out.println("Received Deposit Date: " + customer.getDeposits().getDepositdate());
        } else {
            System.out.println("Deposits is null");
        }

        Customer createdCustomer = customerService.createCustomer(customer);
        return ResponseEntity.ok(createdCustomer);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Integer id) {
        Customer customer = customerService.getCustomerById(id);
        if (customer != null) {
            return ResponseEntity.ok(customer);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/phases")
    public ResponseEntity<List<Phase>> getPhasesByCustomerId(@PathVariable Integer id) {
        List<Phase> phases = phaseService.getPhasesByCustomerId(id);
        if (!phases.isEmpty()) {
            return ResponseEntity.ok(phases);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Customer>> searchCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String number) {

        List<Customer> customers = customerService.searchCustomers(name, number);
        return ResponseEntity.ok(customers);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Integer id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{customerId}/pending-phases")
    public ResponseEntity<List<Phase>> getPendingPhases(@PathVariable Integer customerId) {
        List<Phase> pendingPhases = customerService.getPendingPhases(customerId);
        if (pendingPhases != null) {
            return ResponseEntity.ok(pendingPhases);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

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

        if (loan == null) {
            loan = new Loan();
        }

        loan.setLoandate(updatedLoan.getLoandate());
        loan.setLoanbank(updatedLoan.getLoanbank());
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

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Integer id, @RequestBody Customer updatedCustomer) {
        // updatedCustomer로 들어온 데이터 확인 로깅
        System.out.println("========== Updated Customer JSON Data ==========");
        System.out.println(updatedCustomer); // Customer 엔티티의 toString() 사용
        if (updatedCustomer.getAttachments() != null) {
            System.out.println("Prize Name: " + updatedCustomer.getAttachments().getPrizename());
            System.out.println("Prize Date: " + updatedCustomer.getAttachments().getPrizedate());
        }
        System.out.println("===============================================");

        Customer existingCustomer = customerService.getCustomerById(id);
        if (existingCustomer == null) {
            return ResponseEntity.notFound().build();
        }

        existingCustomer.setCustomertype(updatedCustomer.getCustomertype());
        existingCustomer.setType(updatedCustomer.getType());
        existingCustomer.setGroupname(updatedCustomer.getGroupname());
        existingCustomer.setTurn(updatedCustomer.getTurn());
        existingCustomer.setBatch(updatedCustomer.getBatch());
        existingCustomer.setRegisterdate(updatedCustomer.getRegisterdate());
        existingCustomer.setRegisterprice(updatedCustomer.getRegisterprice());
        existingCustomer.setAdditional(updatedCustomer.getAdditional());
        existingCustomer.setRegisterpath(updatedCustomer.getRegisterpath());
        existingCustomer.setSpecialnote(updatedCustomer.getSpecialnote());
        existingCustomer.setPrizewinning(updatedCustomer.getPrizewinning());

        existingCustomer.getCustomerData().setName(updatedCustomer.getCustomerData().getName());
        existingCustomer.getCustomerData().setPhone(updatedCustomer.getCustomerData().getPhone());
        existingCustomer.getCustomerData().setResnumfront(updatedCustomer.getCustomerData().getResnumfront());
        existingCustomer.getCustomerData().setResnumback(updatedCustomer.getCustomerData().getResnumback());
        existingCustomer.getCustomerData().setEmail(updatedCustomer.getCustomerData().getEmail());

        existingCustomer.getLegalAddress().setPostnumber(updatedCustomer.getLegalAddress().getPostnumber());
        existingCustomer.getLegalAddress().setPost(updatedCustomer.getLegalAddress().getPost());
        existingCustomer.getLegalAddress().setDetailaddress(updatedCustomer.getLegalAddress().getDetailaddress());

        existingCustomer.getPostreceive().setPostnumberreceive(updatedCustomer.getPostreceive().getPostnumberreceive());
        existingCustomer.getPostreceive().setPostreceive(updatedCustomer.getPostreceive().getPostreceive());
        existingCustomer.getPostreceive().setDetailaddressreceive(updatedCustomer.getPostreceive().getDetailaddressreceive());

        existingCustomer.getFinancial().setBankname(updatedCustomer.getFinancial().getBankname());
        existingCustomer.getFinancial().setAccountnum(updatedCustomer.getFinancial().getAccountnum());
        existingCustomer.getFinancial().setAccountholder(updatedCustomer.getFinancial().getAccountholder());

        existingCustomer.getDeposits().setDepositdate(updatedCustomer.getDeposits().getDepositdate());
        existingCustomer.getDeposits().setDepositammount(updatedCustomer.getDeposits().getDepositammount());

        existingCustomer.getResponsible().setGeneralmanagement(updatedCustomer.getResponsible().getGeneralmanagement());
        existingCustomer.getResponsible().setDivision(updatedCustomer.getResponsible().getDivision());
        existingCustomer.getResponsible().setTeam(updatedCustomer.getResponsible().getTeam());
        existingCustomer.getResponsible().setManagername(updatedCustomer.getResponsible().getManagername());

        existingCustomer.getMgm().setMgmcompanyname(updatedCustomer.getMgm().getMgmcompanyname());
        existingCustomer.getMgm().setMgmname(updatedCustomer.getMgm().getMgmname());
        existingCustomer.getMgm().setMgminstitution(updatedCustomer.getMgm().getMgminstitution());
        existingCustomer.getMgm().setMgmaccount(updatedCustomer.getMgm().getMgmaccount());

        existingCustomer.getAttachments().setIsuploaded(updatedCustomer.getAttachments().getIsuploaded());
        existingCustomer.getAttachments().setSealcertificateprovided(updatedCustomer.getAttachments().getSealcertificateprovided());
        existingCustomer.getAttachments().setSelfsignatureconfirmationprovided(updatedCustomer.getAttachments().getSelfsignatureconfirmationprovided());
        existingCustomer.getAttachments().setCommitmentletterprovided(updatedCustomer.getAttachments().getCommitmentletterprovided());
        existingCustomer.getAttachments().setIdcopyprovided(updatedCustomer.getAttachments().getIdcopyprovided());
        existingCustomer.getAttachments().setFreeoption(updatedCustomer.getAttachments().getFreeoption());
        existingCustomer.getAttachments().setForfounding(updatedCustomer.getAttachments().getForfounding());
        existingCustomer.getAttachments().setAgreement(updatedCustomer.getAttachments().getAgreement());
        existingCustomer.getAttachments().setPreferenceattachment(updatedCustomer.getAttachments().getPreferenceattachment());
        existingCustomer.getAttachments().setPrizeattachment(updatedCustomer.getAttachments().getPrizeattachment());
        existingCustomer.getAttachments().setExemption7(updatedCustomer.getAttachments().getExemption7());
        existingCustomer.getAttachments().setInvestmentfile(updatedCustomer.getAttachments().getInvestmentfile());
        existingCustomer.getAttachments().setContract(updatedCustomer.getAttachments().getContract());
        existingCustomer.getAttachments().setFileinfo(updatedCustomer.getAttachments().getFileinfo());

        // 새로 추가된 필드도 설정
        existingCustomer.getAttachments().setPrizename(updatedCustomer.getAttachments().getPrizename());
        existingCustomer.getAttachments().setPrizedate(updatedCustomer.getAttachments().getPrizedate());

        customerService.saveCustomer(existingCustomer);

        return ResponseEntity.ok(existingCustomer);
    }

    @GetMapping("/count/contracted")
    public ResponseEntity<Long> countContractedCustomers() {
        long count = customerService.countContractedCustomers();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/fullypaid")
    public ResponseEntity<Long> countFullyPaidCustomers() {
        long count = customerService.countFullyPaidOrNotOverdueCustomers();
        return ResponseEntity.ok(count);
    }
}
