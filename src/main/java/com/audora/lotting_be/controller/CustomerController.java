package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.model.customer.minor.*;
import com.audora.lotting_be.payload.response.MessageResponse;
import com.audora.lotting_be.service.CustomerService;
import com.audora.lotting_be.service.PhaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        Customer createdCustomer = customerService.createCustomer(customer,true);
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
            @RequestParam(required = false) String number
    ) {
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
    public ResponseEntity<?> getLoanByCustomerId(@PathVariable Integer id) {
        Customer customer = customerService.getCustomerById(id);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(customer.getLoan());
    }

    /**
     * [수정됨] 대출/자납 업데이트:
     * 1) Loan 필드 업데이트
     * 2) customerService.saveCustomer(...)
     * 3) customerService.recalculateEverything(...)
     */
    @PutMapping("/{id}/loan")
    public ResponseEntity<Customer> updateLoanByCustomerId(@PathVariable Integer id, @RequestBody Loan updatedLoan) {
        Customer customer = customerService.getCustomerById(id);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }

        Loan loan = customer.getLoan();
        if (loan == null) {
            loan = new Loan();
        }
        loan.setLoandate(updatedLoan.getLoandate());
        loan.setLoanbank(updatedLoan.getLoanbank());
        loan.setLoanammount(updatedLoan.getLoanammount());
        loan.setSelfdate(updatedLoan.getSelfdate());
        loan.setSelfammount(updatedLoan.getSelfammount());
        customer.setLoan(loan);

        System.out.println("============================================");

        // 우선 저장
        customerService.saveCustomer(customer);
        // 전체 재계산
        customerService.recalculateEverything(customer);

        return ResponseEntity.ok(customer);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelCustomer(@PathVariable Integer id) {
        Customer customer = customerService.getCustomerById(id);
        if (customer == null) {
            return ResponseEntity.status(404)
                    .body(new MessageResponse("Error: Customer not found."));
        }
        customer.setCustomertype("x");
        customerService.saveCustomer(customer);
        return ResponseEntity.ok(new MessageResponse("Customer cancelled successfully."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Integer id, @RequestBody Customer updatedCustomer) {
        System.out.println("========== Updated Customer JSON Data ==========");
        System.out.println(updatedCustomer);
        if (updatedCustomer.getAttachments() != null) {
            System.out.println("Prize Name: " + updatedCustomer.getAttachments().getPrizename());
            System.out.println("Prize Date: " + updatedCustomer.getAttachments().getPrizedate());
        }
        System.out.println("===============================================");

        Customer existingCustomer = customerService.getCustomerById(id);
        if (existingCustomer == null) {
            return ResponseEntity.notFound().build();
        }

        // 1. 최상위 필드 업데이트
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
        existingCustomer.setVotemachine(updatedCustomer.getVotemachine());

        // 2. CustomerData 업데이트 (기본 생성되어 있으므로 null이 아님)
        existingCustomer.getCustomerData().setName(updatedCustomer.getCustomerData().getName());
        existingCustomer.getCustomerData().setPhone(updatedCustomer.getCustomerData().getPhone());
        existingCustomer.getCustomerData().setResnumfront(updatedCustomer.getCustomerData().getResnumfront());
        existingCustomer.getCustomerData().setResnumback(updatedCustomer.getCustomerData().getResnumback());
        existingCustomer.getCustomerData().setEmail(updatedCustomer.getCustomerData().getEmail());

        // 3. LegalAddress 업데이트
        existingCustomer.getLegalAddress().setPostnumber(updatedCustomer.getLegalAddress().getPostnumber());
        existingCustomer.getLegalAddress().setPost(updatedCustomer.getLegalAddress().getPost());
        existingCustomer.getLegalAddress().setDetailaddress(updatedCustomer.getLegalAddress().getDetailaddress());

        // 4. Postreceive 업데이트
        existingCustomer.getPostreceive().setPostnumberreceive(updatedCustomer.getPostreceive().getPostnumberreceive());
        existingCustomer.getPostreceive().setPostreceive(updatedCustomer.getPostreceive().getPostreceive());
        existingCustomer.getPostreceive().setDetailaddressreceive(updatedCustomer.getPostreceive().getDetailaddressreceive());

        // 5. Financial 업데이트
        existingCustomer.getFinancial().setBankname(updatedCustomer.getFinancial().getBankname());
        existingCustomer.getFinancial().setAccountnum(updatedCustomer.getFinancial().getAccountnum());
        existingCustomer.getFinancial().setAccountholder(updatedCustomer.getFinancial().getAccountholder());

        // 6. Deposits 업데이트
        existingCustomer.getDeposits().setDepositdate(updatedCustomer.getDeposits().getDepositdate());
        existingCustomer.getDeposits().setDepositammount(updatedCustomer.getDeposits().getDepositammount());

        // 7. Responsible 업데이트 (null 체크)
        if (updatedCustomer.getResponsible() != null) {
            if (existingCustomer.getResponsible() == null) {
                existingCustomer.setResponsible(new Responsible());
            }
            existingCustomer.getResponsible().setGeneralmanagement(updatedCustomer.getResponsible().getGeneralmanagement());
            existingCustomer.getResponsible().setDivision(updatedCustomer.getResponsible().getDivision());
            existingCustomer.getResponsible().setTeam(updatedCustomer.getResponsible().getTeam());
            existingCustomer.getResponsible().setManagername(updatedCustomer.getResponsible().getManagername());
        }

        // 8. Dahim 업데이트
        if (updatedCustomer.getDahim() != null) {
            if (existingCustomer.getDahim() == null) {
                existingCustomer.setDahim(new Dahim());
            }
            existingCustomer.getDahim().setDahimsisang(updatedCustomer.getDahim().getDahimsisang());
            existingCustomer.getDahim().setDahimdate(updatedCustomer.getDahim().getDahimdate());
            existingCustomer.getDahim().setDahimprepaid(updatedCustomer.getDahim().getDahimprepaid());
            existingCustomer.getDahim().setDahimfirst(updatedCustomer.getDahim().getDahimfirst());
            existingCustomer.getDahim().setDahimfirstpay(updatedCustomer.getDahim().getDahimfirstpay());
            existingCustomer.getDahim().setDahimdate2(updatedCustomer.getDahim().getDahimdate2());
            existingCustomer.getDahim().setDahimsource(updatedCustomer.getDahim().getDahimsource());
            existingCustomer.getDahim().setDahimsecond(updatedCustomer.getDahim().getDahimsecond());
            existingCustomer.getDahim().setDahimsecondpay(updatedCustomer.getDahim().getDahimsecondpay());
            existingCustomer.getDahim().setDahimdate3(updatedCustomer.getDahim().getDahimdate3());
            existingCustomer.getDahim().setDahimsum(updatedCustomer.getDahim().getDahimsum());
        }

        // 9. MGM 업데이트
        if (updatedCustomer.getMgm() != null) {
            if (existingCustomer.getMgm() == null) {
                existingCustomer.setMgm(new MGM());
            }
            existingCustomer.getMgm().setMgmcompanyname(updatedCustomer.getMgm().getMgmcompanyname());
            existingCustomer.getMgm().setMgmname(updatedCustomer.getMgm().getMgmname());
            existingCustomer.getMgm().setMgminstitution(updatedCustomer.getMgm().getMgminstitution());
            existingCustomer.getMgm().setMgmaccount(updatedCustomer.getMgm().getMgmaccount());
        }

        // 10. Firstemp 업데이트
        if (updatedCustomer.getFirstemp() != null) {
            if (existingCustomer.getFirstemp() == null) {
                existingCustomer.setFirstemp(new Firstemp());
            }
            existingCustomer.getFirstemp().setFirstemptimes(updatedCustomer.getFirstemp().getFirstemptimes());
            existingCustomer.getFirstemp().setFirstempdate(updatedCustomer.getFirstemp().getFirstempdate());
        }

        // 11. Secondemp 업데이트
        if (updatedCustomer.getSecondemp() != null) {
            if (existingCustomer.getSecondemp() == null) {
                existingCustomer.setSecondemp(new Secondemp());
            }
            existingCustomer.getSecondemp().setSecondemptimes(updatedCustomer.getSecondemp().getSecondemptimes());
            existingCustomer.getSecondemp().setSecondempdate(updatedCustomer.getSecondemp().getSecondempdate());
        }

        // 12. Meetingattend 업데이트
        if (updatedCustomer.getMeetingattend() != null) {
            if (existingCustomer.getMeetingattend() == null) {
                existingCustomer.setMeetingattend(new Meetingattend());
            }
            existingCustomer.getMeetingattend().setHowtoattend(updatedCustomer.getMeetingattend().getHowtoattend());
            existingCustomer.getMeetingattend().setFtofattend(updatedCustomer.getMeetingattend().getFtofattend());
            existingCustomer.getMeetingattend().setSelfattend(updatedCustomer.getMeetingattend().getSelfattend());
            existingCustomer.getMeetingattend().setBehalfattend(updatedCustomer.getMeetingattend().getBehalfattend());
        }

        // 13. Agenda 업데이트
        if (updatedCustomer.getAgenda() != null) {
            if (existingCustomer.getAgenda() == null) {
                existingCustomer.setAgenda(new Agenda());
            }
            existingCustomer.getAgenda().setAgenda1(updatedCustomer.getAgenda().getAgenda1());
            existingCustomer.getAgenda().setAgenda2_1(updatedCustomer.getAgenda().getAgenda2_1());
            existingCustomer.getAgenda().setAgenda2_2(updatedCustomer.getAgenda().getAgenda2_2());
            existingCustomer.getAgenda().setAgenda2_3(updatedCustomer.getAgenda().getAgenda2_3());
            existingCustomer.getAgenda().setAgenda2_4(updatedCustomer.getAgenda().getAgenda2_4());
            existingCustomer.getAgenda().setAgenda3(updatedCustomer.getAgenda().getAgenda3());
            existingCustomer.getAgenda().setAgenda4(updatedCustomer.getAgenda().getAgenda4());
            existingCustomer.getAgenda().setAgenda5(updatedCustomer.getAgenda().getAgenda5());
            existingCustomer.getAgenda().setAgenda6(updatedCustomer.getAgenda().getAgenda6());
            existingCustomer.getAgenda().setAgenda7(updatedCustomer.getAgenda().getAgenda7());
            existingCustomer.getAgenda().setAgenda8(updatedCustomer.getAgenda().getAgenda8());
            existingCustomer.getAgenda().setAgenda9(updatedCustomer.getAgenda().getAgenda9());
            existingCustomer.getAgenda().setAgenda10(updatedCustomer.getAgenda().getAgenda10());
        }

        // 14. Attachments 업데이트 (첨부파일, 체크박스, 사은품 관련 등)\n
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
        existingCustomer.getAttachments().setPrizename(updatedCustomer.getAttachments().getPrizename());
        existingCustomer.getAttachments().setPrizedate(updatedCustomer.getAttachments().getPrizedate());

        customerService.saveCustomer(existingCustomer);
        customerService.recalculateEverything(existingCustomer);

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
