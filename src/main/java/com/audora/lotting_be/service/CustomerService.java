// CustomerService.java
package com.audora.lotting_be.service;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.Fee.Fee;
import com.audora.lotting_be.model.Fee.FeePerPhase;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.model.customer.Status;
import com.audora.lotting_be.repository.CustomerRepository;
import com.audora.lotting_be.repository.FeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private FeeRepository feeRepository;

    public Integer getNextCustomerId() {
        return customerRepository.getNextId();
    }

    public Customer getCustomerById(Integer id) {
        Optional<Customer> optionalCustomer = customerRepository.findById(id);
        return optionalCustomer.orElse(null);
    }

    public Customer createCustomer(Customer customer) {

        Fee fee = feeRepository.findByGroupnameAndBatch(
                customer.getType(), customer.getBatch()); // 유저테이블 타입 = 차수테이블 군, 유저테이블 batch = 차수

        if (fee != null) {
            List<FeePerPhase> feePerPhases = fee.getFeePerPhases();
            List<Phase> phases = new ArrayList<>();

            for (FeePerPhase feePerPhase : feePerPhases) {
                Phase phase = new Phase();
                phase.setPhaseNumber(feePerPhase.getPhaseNumber());
                phase.setCharge(feePerPhase.getPhasefee());

                LocalDate plannedDate = calculatePlannedDate(
                        customer.getRegisterdate(), feePerPhase.getPhasedate());
                phase.setPlanneddate(plannedDate);

                phase.setCustomer(customer);
                phases.add(phase);
            }
            customer.setPhases(phases);
        }

        // Status 객체가 null인 경우 새로운 Status 객체 생성
        if (customer.getStatus() == null) {
            Status status = new Status();
            status.setCustomer(customer); // 양방향 관계 설정
            customer.setStatus(status);
        }

        return customerRepository.save(customer);
    }

    private LocalDate calculatePlannedDate(LocalDate registerDate, String phasedate) {
        if (phasedate.endsWith("달") || phasedate.endsWith("개월")) {
            int months = Integer.parseInt(phasedate.replaceAll("[^0-9]", ""));
            return registerDate.plusMonths(months);
        } else if (phasedate.endsWith("년")) {
            int years = Integer.parseInt(phasedate.replaceAll("[^0-9]", ""));
            return registerDate.plusYears(years);
        } else {
            return registerDate;
        }
    }
}
