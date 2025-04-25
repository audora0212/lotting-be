package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.payload.request.PhaseModificationRequest;
import com.audora.lotting_be.service.CustomerService;
import com.audora.lotting_be.service.PhaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/phases")
public class PhaseController {

    @Autowired
    private PhaseService phaseService;

    @Autowired
    private CustomerService customerService;


    @PutMapping("/{id}")
    public ResponseEntity<Phase> updatePhase(@PathVariable Long id, @RequestBody Phase phaseDetails) {
        Optional<Phase> optionalPhase = phaseService.getPhaseById(id);
        if (!optionalPhase.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Phase phase = optionalPhase.get();
        phase.setPlanneddate(phaseDetails.getPlanneddate());
        phase.setFullpaiddate(phaseDetails.getFullpaiddate());
        phase.setCharge(phaseDetails.getCharge());
        phase.setDiscount(phaseDetails.getDiscount());
        phase.setExemption(phaseDetails.getExemption());
        phase.setService(phaseDetails.getService());
        phase.setMove(phaseDetails.getMove());
        phase.setFeesum(phaseDetails.getFeesum());
        phase.setCharged(phaseDetails.getCharged());
        phase.setSum(phaseDetails.getSum());

        Phase updatedPhase = phaseService.savePhase(phase);

        Customer customer = updatedPhase.getCustomer();
        customerService.updateStatusFields(customer);
        customerService.saveCustomer(customer);

        return ResponseEntity.ok(updatedPhase);
    }


    @GetMapping("/{id}")
    public ResponseEntity<Phase> getPhaseById(@PathVariable Long id) {
        Optional<Phase> optionalPhase = phaseService.getPhaseById(id);
        return optionalPhase.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @PutMapping("/customer/{customerId}/phase/{phaseNumber}/modify")
    public ResponseEntity<Phase> modifyPhaseByCustomerAndPhaseNumber(@PathVariable Integer customerId,
                                                                     @PathVariable Integer phaseNumber,
                                                                     @RequestBody PhaseModificationRequest request) {

        Customer customer = customerService.getCustomerById(customerId);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }

        Phase phase = null;
        if (customer.getPhases() != null) {
            phase = customer.getPhases().stream()
                    .filter(p -> p.getPhaseNumber() != null && p.getPhaseNumber().equals(phaseNumber))
                    .findFirst().orElse(null);
        }
        if (phase == null) {
            return ResponseEntity.notFound().build();
        }

        phase.setCharge(request.getCharge());
        phase.setService(request.getService());
        phase.setDiscount(request.getDiscount());
        phase.setExemption(request.getExemption());
        phase.setMove(request.getMove());

        customerService.recalculateEverything(customer);

        return ResponseEntity.ok(phase);
    }
}
