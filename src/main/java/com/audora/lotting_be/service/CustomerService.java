// src/main/java/com/audora/lotting_be/service/CustomerService.java
package com.audora.lotting_be.service;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.Fee.Fee;
import com.audora.lotting_be.model.Fee.FeePerPhase;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.model.customer.Status;
import com.audora.lotting_be.payload.response.LateFeeInfo;
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

        Fee fee = feeRepository.findByGroupnameAndBatch(
                customer.getType() + customer.getGroupname(), customer.getBatch()); // 유저테이블 타입 = 차수테이블 군, 유저테이블 batch = 차수

        if (fee != null) {
            List<FeePerPhase> feePerPhases = fee.getFeePerPhases();
            List<Phase> phases = new ArrayList<>();

            for (FeePerPhase feePerPhase : feePerPhases) {
                Phase phase = new Phase();
                phase.setPhaseNumber(feePerPhase.getPhaseNumber());
                phase.setCharge(feePerPhase.getPhasefee());

                // discount과 exemption이 null일 수 있으므로 안전하게 처리
                Long discount = 0L;
                Long exemption = 0L;
                Long service = 0L;

                // feesum 계산: charge - discount - exemption + service
                Long feesum = feePerPhase.getPhasefee() - discount - exemption + service;
                phase.setFeesum(feesum);

                // sum을 charge로 초기화 (기존 로직 유지)
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

    /**
     * Status 객체의 필드를 업데이트하는 메서드
     */
    public void updateStatusFields(Customer customer) {
        Status status = customer.getStatus();

        // exemptionsum: 모든 Phase의 exemption 합
        Long exemptionsum = customer.getPhases().stream()
                .mapToLong(phase -> phase.getExemption() != null ? phase.getExemption() : 0L)
                .sum();
        status.setExemptionsum(exemptionsum);

        // unpaidammout: 모든 Phase의 sum 합
        Long unpaidammout = customer.getPhases().stream()
                .mapToLong(phase -> phase.getSum() != null ? phase.getSum() : 0L)
                .sum();
        status.setUnpaidammout(unpaidammout);



        // unpaidphase: 미납된 Phase의 phaseNumber들을 콤마로 구분하여 저장
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

        // ammountsum: 모든 Phase의 feesum 합
        Long ammountsum = customer.getPhases().stream()
                .mapToLong(phase -> phase.getFeesum() != null ? phase.getFeesum() : 0L)
                .sum();
        status.setAmmountsum(ammountsum);


        // Status 객체 저장
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

    /**
     * 검색 기준에 따라 연체료 정보를 가져옵니다.
     *
     * @param name   검색할 이름 (선택 사항)
     * @param number 검색할 회원번호 (선택 사항)
     * @return LateFeeInfo 리스트
     */
    public List<LateFeeInfo> getLateFeeInfos(String name, String number) {
        List<Customer> customers;

        // 검색 조건에 따라 고객 리스트 가져오기
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
            System.out.println("Processing Customer ID: " + customer.getId());

            List<Phase> phases = customer.getPhases();

            if (phases == null || phases.isEmpty()) {
                continue; // 차수가 없는 고객은 건너뜁니다.
            }

            // 미납 Phase 기준: plannedDate < today && fullpaiddate == null
            List<Phase> unpaidPhases = phases.stream()
                    .filter(phase -> phase.getPlanneddate() != null
                            && phase.getPlanneddate().isBefore(today)
                            && phase.getFullpaiddate() == null)
                    .collect(Collectors.toList());

            // 미납 일자 출력
            unpaidPhases.forEach(phase -> {
                System.out.println("Unpaid Phase for Customer ID " + customer.getId() + ": Planned Date = " + phase.getPlanneddate());
            });

            LateFeeInfo info = new LateFeeInfo();
            info.setId(customer.getId());

            if (unpaidPhases.isEmpty()) {
                // 미납 Phase가 없는 경우 (완납된 고객)
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
                info.setPaidAmount(paidAmount != null ? paidAmount : 0L);
                info.setLateFee(0.0);
                info.setTotalOwed(0L);
                lateFeeInfos.add(info);
                continue;
            }

            // 미납 Phase가 있는 경우
            // 마지막 미납 Phase Number
            Optional<Phase> lastUnpaidPhaseOpt = unpaidPhases.stream()
                    .max(Comparator.comparing(Phase::getPhaseNumber));

            Integer lastUnpaidPhaseNumber = lastUnpaidPhaseOpt.map(Phase::getPhaseNumber).orElse(null);
            info.setLastUnpaidPhaseNumber(lastUnpaidPhaseNumber);

            // 가장 오래된 plannedDate
            Optional<Phase> earliestUnpaidPhaseOpt = unpaidPhases.stream()
                    .min(Comparator.comparing(Phase::getPlanneddate));

            LocalDate lateBaseDate = earliestUnpaidPhaseOpt.map(Phase::getPlanneddate).orElse(null);
            info.setLateBaseDate(lateBaseDate);

            // 최근 납부일자: 완납된 Phase 중 가장 최근 fullpaiddate
            Optional<Phase> recentPaymentOpt = phases.stream()
                    .filter(phase -> phase.getFullpaiddate() != null)
                    .max(Comparator.comparing(Phase::getFullpaiddate));

            LocalDate recentPaymentDate = recentPaymentOpt.map(Phase::getFullpaiddate).orElse(null);
            info.setRecentPaymentDate(recentPaymentDate);

            // 연체 일수
            Long daysOverdue = (lateBaseDate != null) ? ChronoUnit.DAYS.between(lateBaseDate, today) : 0L;
            info.setDaysOverdue(daysOverdue != null ? daysOverdue : 0L);

            // 연체금액: 미납 Phase의 sum 합
            Long overdueAmount = unpaidPhases.stream()
                    .mapToLong(phase -> phase.getSum() != null ? phase.getSum() : 0L)
                    .sum();
            info.setOverdueAmount(overdueAmount != null ? overdueAmount : 0L);

            // 납입금액: 모든 Phase의 charged 합
            Long paidAmount = phases.stream()
                    .mapToLong(phase -> phase.getCharged() != null ? phase.getCharged() : 0L)
                    .sum();
            info.setPaidAmount(paidAmount != null ? paidAmount : 0L);

            // 연체율: overdueAmount / totalFeesum * 100
            Long totalFeesum = phases.stream()
                    .mapToLong(phase -> phase.getFeesum() != null ? phase.getFeesum() : 0L)
                    .sum();

            Double lateRate = (totalFeesum != 0) ? ((double) overdueAmount / totalFeesum) * 100 : 0.0;
            info.setLateRate(lateRate != null ? Math.round(lateRate * 100.0) / 100.0 : 0.0);

            // 연체료: overdueAmount * 1.4%
            Double lateFee = overdueAmount * 0.014;
            info.setLateFee(lateFee != null ? Math.round(lateFee * 100.0) / 100.0 : 0.0);

            // 내야할 돈 합계: overdueAmount + lateFee
            Long totalOwed = Math.round(overdueAmount + lateFee);
            info.setTotalOwed(totalOwed != null ? totalOwed : 0L);

            // 기타 필드 null 체크
            info.setCustomertype(customer.getCustomertype() != null ? customer.getCustomertype() : "N/A");
            info.setName(customer.getCustomerData() != null && customer.getCustomerData().getName() != null
                    ? customer.getCustomerData().getName()
                    : "N/A");
            info.setRegisterdate(customer.getRegisterdate() != null ? customer.getRegisterdate() : null);

            lateFeeInfos.add(info);
        }

        return lateFeeInfos;
    }
}
