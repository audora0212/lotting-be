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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
                customer.getType()+customer.getGroupname(), customer.getBatch()); // 유저테이블 타입 = 차수테이블 군, 유저테이블 batch = 차수

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

    public List<Customer> searchCustomers(String name, String number) {
        if (name != null && number != null) {
            return customerRepository.findByCustomerDataNameAndId(name, Integer.parseInt(number));
        } else if (name != null) {
            return customerRepository.findByCustomerDataNameContaining(name);
        } else if (number != null) {
            return customerRepository.findById(Integer.parseInt(number))
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        } else {
            return customerRepository.findAll();
        }
    }

    public void deleteCustomer(Integer id) {
        customerRepository.deleteById(id);
    }

    public List<Phase> getPendingPhases(Integer customerId) {
        Optional<Customer> customerOptional = customerRepository.findById(customerId);
        if (customerOptional.isPresent()) {
            Customer customer = customerOptional.get();
            List<Phase> phases = customer.getPhases();
            // sum 필드가 0이 아닌 Phase들만 필터링
            List<Phase> pendingPhases = phases.stream()
                    .filter(phase -> phase.getSum() != null && phase.getSum() > 0)
                    .collect(Collectors.toList());
            return pendingPhases;
        } else {
            return null;
        }
    }

    public List<Phase> getCompletedPhases(Integer customerId) {
        Optional<Customer> customerOptional = customerRepository.findById(customerId);
        if (customerOptional.isPresent()) {
            Customer customer = customerOptional.get();
            List<Phase> phases = customer.getPhases();
            // sum 필드가 0인 Phase들만 필터링 (완납된 Phase)
            List<Phase> completedPhases = phases.stream()
                    .filter(phase -> phase.getSum() == null || phase.getSum() == 0)
                    .collect(Collectors.toList());
            return completedPhases;
        } else {
            return null;
        }
    }

    public Customer saveCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    /**
     * 고객 해약 처리
     */
    public boolean cancelCustomer(Integer id) {
        Optional<Customer> optionalCustomer = customerRepository.findById(id);
        if (optionalCustomer.isPresent()) {
            Customer customer = optionalCustomer.get();
            customer.setCustomertype("c");
            customerRepository.save(customer);
            return true;
        } else {
            return false;
        }
    }


}
