// src/main/java/com/audora/lotting_be/service/CustomerService.java
package com.audora.lotting_be.service;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.model.customer.Status;
import com.audora.lotting_be.payload.response.LateFeeInfo;
import com.audora.lotting_be.model.Fee.Fee;
import com.audora.lotting_be.model.Fee.FeePerPhase;
import com.audora.lotting_be.repository.CustomerRepository;
import com.audora.lotting_be.repository.FeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
        // ID 중복 확인
        if (customerRepository.existsById(customer.getId())) {
            throw new IllegalArgumentException("이미 존재하는 관리번호입니다.");
        }

        Fee fee = feeRepository.findByGroupnameAndBatch(
                customer.getType() + customer.getGroupname(), customer.getBatch());

        if (fee != null) {
            List<FeePerPhase> feePerPhases = fee.getFeePerPhases();
            List<Phase> phases = new ArrayList<>();

            for (FeePerPhase feePerPhase : feePerPhases) {
                Phase phase = new Phase();
                phase.setPhaseNumber(feePerPhase.getPhaseNumber());
                phase.setCharge(feePerPhase.getPhasefee());

                Long discount = 0L;
                Long exemption = 0L;
                Long service = 0L;

                Long feesum = feePerPhase.getPhasefee() - discount - exemption + service;
                phase.setFeesum(feesum);

                // sum을 charge로 초기화
                phase.setSum(feePerPhase.getPhasefee());

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

        // Phase 생성 후 Status 필드 업데이트
        updateStatusFields(customer);

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

    public void updateStatusFields(Customer customer) {
        Status status = customer.getStatus();

        Long exemptionsum = customer.getPhases().stream()
                .mapToLong(phase -> phase.getExemption() != null ? phase.getExemption() : 0L)
                .sum();
        status.setExemptionsum(exemptionsum);

        Long unpaidammout = customer.getPhases().stream()
                .mapToLong(phase -> phase.getSum() != null ? phase.getSum() : 0L)
                .sum();
        status.setUnpaidammout(unpaidammout);

        List<Integer> unpaidPhaseNumbers = customer.getPhases().stream()
                .filter(phase -> phase.getPlanneddate() != null
                        && phase.getPlanneddate().isBefore(LocalDate.now())
                        && phase.getFullpaiddate() == null)
                .map(Phase::getPhaseNumber)
                .sorted()
                .collect(Collectors.toList());

        String unpaidPhaseStr = unpaidPhaseNumbers.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        status.setUnpaidphase(unpaidPhaseStr);

        Long ammountsum = customer.getPhases().stream()
                .mapToLong(phase -> phase.getFeesum() != null ? phase.getFeesum() : 0L)
                .sum();
        status.setAmmountsum(ammountsum);

        customer.setStatus(status);
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
            return phases.stream()
                    .filter(phase -> phase.getSum() != null && phase.getSum() > 0)
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }

    public List<Phase> getCompletedPhases(Integer customerId) {
        Optional<Customer> customerOptional = customerRepository.findById(customerId);
        if (customerOptional.isPresent()) {
            Customer customer = customerOptional.get();
            List<Phase> phases = customer.getPhases();
            return phases.stream()
                    .filter(phase -> phase.getSum() == null || phase.getSum() == 0)
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }

    public Customer saveCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

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

    public List<LateFeeInfo> getLateFeeInfos(String name, String number) {
        List<Customer> customers;

        if (name != null && !name.isEmpty() && number != null && !number.isEmpty()) {
            try {
                Integer id = Integer.parseInt(number);
                customers = customerRepository.findByCustomerDataNameAndId(name, id);
            } catch (NumberFormatException e) {
                customers = Collections.emptyList();
            }
        } else if (name != null && !name.isEmpty()) {
            customers = customerRepository.findByCustomerDataNameContaining(name);
        } else if (number != null && !number.isEmpty()) {
            try {
                Integer id = Integer.parseInt(number);
                customers = customerRepository.findById(id)
                        .map(Collections::singletonList)
                        .orElse(Collections.emptyList());
            } catch (NumberFormatException e) {
                customers = Collections.emptyList();
            }
        } else {
            customers = customerRepository.findAll();
        }

        List<LateFeeInfo> lateFeeInfos = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Customer customer : customers) {
            List<Phase> phases = customer.getPhases();
            if (phases == null || phases.isEmpty()) {
                continue;
            }

            List<Phase> unpaidPhases = phases.stream()
                    .filter(phase -> phase.getPlanneddate() != null
                            && phase.getPlanneddate().isBefore(today)
                            && phase.getFullpaiddate() == null)
                    .collect(Collectors.toList());

            LateFeeInfo info = new LateFeeInfo();
            info.setId(customer.getId());

            if (unpaidPhases.isEmpty()) {
                // 미납 없는 회원
                info.setLastUnpaidPhaseNumber(null);
                info.setCustomertype(customer.getCustomertype() != null ? customer.getCustomertype() : "N/A");
                info.setName(customer.getCustomerData() != null && customer.getCustomerData().getName() != null
                        ? customer.getCustomerData().getName()
                        : "N/A");
                info.setRegisterdate(customer.getRegisterdate() != null ? customer.getRegisterdate() : null);
                info.setLateBaseDate(null);
                info.setRecentPaymentDate(null);
                info.setDaysOverdue(0L);
                info.setLateRate(0.0);
                info.setOverdueAmount(0L);
                Long paidAmount = phases.stream()
                        .mapToLong(phase -> phase.getCharged() != null ? phase.getCharged() : 0L)
                        .sum();
                info.setPaidAmount(paidAmount);
                info.setLateFee(0.0);
                info.setTotalOwed(0L);
                lateFeeInfos.add(info);
                continue;
            }

            // 미납 회원 로직은 생략 (기존과 동일)
            // ...
        }

        return lateFeeInfos;
    }

    // 정계약한 사람들(customertype = 'c')의 숫자 반환
    public long countContractedCustomers() {
        return customerRepository.countByCustomertype("c");
    }

    // 미납이 아닌(모든 예정금액 납부완료 또는 예정일이 아직 지나지 않은) 회원 수 반환
    public long countFullyPaidOrNotOverdueCustomers() {
        List<Customer> allCustomers = customerRepository.findAll();
        LocalDate today = LocalDate.now();

        return allCustomers.stream()
                .filter(customer -> {
                    List<Phase> phases = customer.getPhases();
                    if (phases == null || phases.isEmpty()) {
                        // 차수 정보가 없으면 미납은 아니라고 가정
                        return true;
                    }

                    // 미납 기준: planneddate < today && fullpaiddate == null
                    // 미납 phase가 하나라도 있으면 false
                    boolean hasOverdue = phases.stream()
                            .anyMatch(phase -> phase.getPlanneddate() != null
                                    && phase.getPlanneddate().isBefore(today)
                                    && phase.getFullpaiddate() == null);

                    return !hasOverdue; // 미납이 아닌 경우 true
                })
                .count();
    }
}
