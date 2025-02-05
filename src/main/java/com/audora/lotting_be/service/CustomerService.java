package com.audora.lotting_be.service;

import com.audora.lotting_be.model.Fee.Fee;
import com.audora.lotting_be.model.Fee.FeePerPhase;
import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.model.customer.Status;
import com.audora.lotting_be.payload.response.CustomerDepositDTO;
import com.audora.lotting_be.payload.response.LateFeeInfo;
import com.audora.lotting_be.repository.CustomerRepository;
import com.audora.lotting_be.repository.FeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private FeeRepository feeRepository;

    // 고객 ID 생성
    public Integer getNextCustomerId() {
        return customerRepository.getNextId();
    }

    // 고객 조회
    public Customer getCustomerById(Integer id) {
        Optional<Customer> optionalCustomer = customerRepository.findById(id);
        return optionalCustomer.orElse(null);
    }

    // 신규 고객 생성 시 Fee 조회 및 Phase 초기화 후 전체 분배 재계산
    public Customer createCustomer(Customer customer) {
        if (customerRepository.existsById(customer.getId())) {
            throw new IllegalArgumentException("이미 존재하는 관리번호입니다.");
        }
        // Fee 조회 및 Phase 초기화
        Fee fee = feeRepository.findByGroupnameAndBatch(
                customer.getType() + customer.getGroupname(),
                customer.getBatch()
        );
        if (fee != null) {
            List<FeePerPhase> feePerPhases = fee.getFeePerPhases();
            List<Phase> phases = new ArrayList<>();
            for (FeePerPhase feePerPhase : feePerPhases) {
                Phase phase = new Phase();
                phase.setPhaseNumber(feePerPhase.getPhaseNumber());
                Long charge = feePerPhase.getPhasefee();
                phase.setCharge(charge);
                Long service = 0L;
                Long exemption = 0L;
                phase.setService(service);
                phase.setExemption(exemption);
                Long feesum = charge + service - exemption;
                phase.setFeesum(feesum);
                phase.setCharged(0L);
                phase.setSum(feesum);
                phase.setPlanneddateString(feePerPhase.getPhasedate());
                LocalDate plannedDate = calculatePlannedDate(customer.getRegisterdate(), feePerPhase.getPhasedate());
                phase.setPlanneddate(plannedDate);
                phase.setFullpaiddate(null);
                phase.setCustomer(customer);
                phases.add(phase);
            }
            customer.setPhases(phases);
        }
        if (customer.getStatus() == null) {
            Status status = new Status();
            status.setCustomer(customer);
            customer.setStatus(status);
        }
        // 전체 분배 재계산(예약금 및 대출/자납액)
        recalculatePaymentDistribution(customer);
        updateStatusFields(customer);
        return customerRepository.save(customer);
    }

    // phasedate 계산: phasedate 문자열에 따라 등록일에 offset을 적용
    private LocalDate calculatePlannedDate(LocalDate registerDate, String phasedate) {
        if (phasedate == null || phasedate.isEmpty()) {
            return registerDate;
        }
        if (phasedate.endsWith("달") || phasedate.endsWith("개월")) {
            int months = Integer.parseInt(phasedate.replaceAll("[^0-9]", ""));
            return registerDate.plusMonths(months);
        } else if (phasedate.endsWith("년")) {
            int years = Integer.parseInt(phasedate.replaceAll("[^0-9]", ""));
            return registerDate.plusYears(years);
        } else {
            return registerDate.plusYears(100);
        }
    }

    // 각 Phase의 납입액(charged) 및 잔액(sum)을 업데이트
    public void updateStatusFields(Customer customer) {
        List<Phase> phases = customer.getPhases();
        if (phases != null) {
            for (Phase phase : phases) {
                long charge = phase.getCharge() != null ? phase.getCharge() : 0L;
                long service = phase.getService() != null ? phase.getService() : 0L;
                long exemption = phase.getExemption() != null ? phase.getExemption() : 0L;
                long feesum = charge + service - exemption;
                phase.setFeesum(feesum);
                long charged = phase.getCharged() != null ? phase.getCharged() : 0L;
                long sum = feesum - charged;
                phase.setSum(sum);
            }
        }
        Status status = customer.getStatus();
        if (status == null) {
            status = new Status();
            status.setCustomer(customer);
            customer.setStatus(status);
        }
        long exemptionsum = phases != null ? phases.stream()
                .mapToLong(p -> p.getExemption() != null ? p.getExemption() : 0L)
                .sum() : 0L;
        status.setExemptionsum(exemptionsum);
        long unpaidammout = phases != null ? phases.stream()
                .mapToLong(p -> p.getSum() != null ? p.getSum() : 0L)
                .sum() : 0L;
        status.setUnpaidammout(unpaidammout);
        List<Integer> unpaidPhaseNumbers = phases != null ? phases.stream()
                .filter(p -> p.getPlanneddate() != null &&
                        p.getPlanneddate().isBefore(LocalDate.now()) &&
                        p.getFullpaiddate() == null)
                .map(Phase::getPhaseNumber)
                .sorted()
                .collect(Collectors.toList()) : new ArrayList<>();
        String unpaidPhaseStr = unpaidPhaseNumbers.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        status.setUnpaidphase(unpaidPhaseStr);
        long ammountsum = phases != null ? phases.stream()
                .mapToLong(p -> p.getFeesum() != null ? p.getFeesum() : 0L)
                .sum() : 0L;
        status.setAmmountsum(ammountsum);
        status.setPercent40((long) (ammountsum * 0.4));
        if (status.getExceedamount() == null) {
            status.setExceedamount(0L);
        }
        if (status.getLoanExceedAmount() == null) {
            status.setLoanExceedAmount(0L);
        }
        customer.setStatus(status);
    }

    /**
     * [공용 메서드] 지정한 paymentAmount를 고객의 Phase들에 순차적으로 분배하고 남은 금액을 반환합니다.
     * 이 메서드는 현재 phase에 이미 적용된 납입액 위에 추가하는 형태로 동작합니다.
     */
    public long distributePaymentToPhases(Customer customer, long paymentAmount, LocalDate paymentDate) {
        List<Phase> phases = customer.getPhases();
        if (phases == null) return paymentAmount;
        phases.sort(Comparator.comparingInt(Phase::getPhaseNumber));
        long remaining = paymentAmount;
        for (Phase phase : phases) {
            long already = phase.getCharged() != null ? phase.getCharged() : 0;
            long required = (phase.getFeesum() != null ? phase.getFeesum() : 0) - already;
            if (required <= 0) continue;
            if (remaining >= required) {
                phase.setCharged(already + required);
                remaining -= required;
                if (phase.getFullpaiddate() == null) {
                    phase.setFullpaiddate(paymentDate);
                }
            } else {
                phase.setCharged(already + remaining);
                remaining = 0;
                break;
            }
        }
        return remaining;
    }

    /**
     * ★ 전체 분배 재계산 메서드 ★
     * 고객의 모든 Phase의 납입액을 0으로 초기화한 후,
     * 예약금(Deposit)과 대출/자납액(Loan)을 순차적으로 분배하여, 최신 분배 내역을 계산합니다.
     *
     * 이 메서드를 호출하면 기존 phase 분배 내역(예를 들어, 처음 5천만원 분배된 내역)을 모두 리셋한 뒤,
     * 업데이트된 예약금 값(예: 5백만원 또는 7천만원)과 대출/자납액을 반영하여 다시 분배합니다.
     */
    public void recalculatePaymentDistribution(Customer customer) {
        // 1. 기존 분배 내역 초기화: 모든 phase의 charged 값을 0, fullpaiddate 초기화
        if (customer.getPhases() != null) {
            for (Phase phase : customer.getPhases()) {
                phase.setCharged(0L);
                phase.setFullpaiddate(null);
                long charge = phase.getCharge() != null ? phase.getCharge() : 0L;
                long service = phase.getService() != null ? phase.getService() : 0L;
                long exemption = phase.getExemption() != null ? phase.getExemption() : 0L;
                long feesum = charge + service - exemption;
                phase.setFeesum(feesum);
                phase.setSum(feesum);
            }
        }
        // 2. 예약금 분배 적용
        long depositAmount = 0;
        LocalDate depositDate = customer.getRegisterdate();
        if (customer.getDeposits() != null && customer.getDeposits().getDepositammount() != null) {
            depositAmount = customer.getDeposits().getDepositammount();
            if (customer.getDeposits().getDepositdate() != null) {
                depositDate = customer.getDeposits().getDepositdate();
            }
        }
        long leftoverDeposit = 0;
        if (depositAmount > 0) {
            leftoverDeposit = distributePaymentToPhases(customer, depositAmount, depositDate);
        }
        if (customer.getStatus() != null) {
            customer.getStatus().setExceedamount(leftoverDeposit);
        }
        // 3. 대출/자납액 분배 적용
        long totalLoanPayment = 0;
        if (customer.getLoan() != null) {
            long loanAmount = customer.getLoan().getLoanammount() != null ? customer.getLoan().getLoanammount() : 0;
            long selfAmount = customer.getLoan().getSelfammount() != null ? customer.getLoan().getSelfammount() : 0;
            totalLoanPayment = loanAmount + selfAmount;
            if (totalLoanPayment > 0) {
                LocalDate loanDate = customer.getLoan().getLoandate() != null ? customer.getLoan().getLoandate() : customer.getRegisterdate();
                long leftoverLoan = distributePaymentToPhases(customer, totalLoanPayment, loanDate);
                customer.getLoan().setLoanselfsum(totalLoanPayment - leftoverLoan);
                customer.getLoan().setLoanselfcurrent(leftoverLoan);
                if (customer.getStatus() != null) {
                    customer.getStatus().setLoanExceedAmount(leftoverLoan);
                }
            } else {
                customer.getLoan().setLoanselfsum(0L);
                customer.getLoan().setLoanselfcurrent(0L);
                if (customer.getStatus() != null) {
                    customer.getStatus().setLoanExceedAmount(0L);
                }
            }
        }
        updateStatusFields(customer);
    }

    // 기존 대출/자납 적용 메서드를 수정하여 전체 재계산 방식으로 처리
    public void applyLoanPayment(Customer customer) {
        recalculatePaymentDistribution(customer);
    }

    /** 정계약한(customertype = 'c') 고객의 수 반환 */
    public long countContractedCustomers() {
        return customerRepository.countByCustomertype("c");
    }

    /** 미납이 아닌(모든 예정금액 납부완료 또는 예정일이 아직 지나지 않은) 고객 수 반환 */
    public long countFullyPaidOrNotOverdueCustomers() {
        List<Customer> allCustomers = customerRepository.findAll();
        LocalDate today = LocalDate.now();
        return allCustomers.stream().filter(customer -> {
            List<Phase> phases = customer.getPhases();
            if (phases == null || phases.isEmpty()) return true;
            boolean hasOverdue = phases.stream().anyMatch(phase ->
                    phase.getPlanneddate() != null &&
                            phase.getPlanneddate().isBefore(today) &&
                            phase.getFullpaiddate() == null);
            return !hasOverdue;
        }).count();
    }

    /** 연체료 정보를 가져오는 메서드 */
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
            if (phases == null || phases.isEmpty()) continue;
            List<Phase> unpaidPhases = phases.stream().filter(phase ->
                    phase.getPlanneddate() != null &&
                            phase.getPlanneddate().isBefore(today) &&
                            phase.getFullpaiddate() == null
            ).collect(Collectors.toList());
            LateFeeInfo info = new LateFeeInfo();
            info.setId(customer.getId());
            info.setCustomertype(customer.getCustomertype() != null ? customer.getCustomertype() : "N/A");
            info.setName(customer.getCustomerData() != null && customer.getCustomerData().getName() != null
                    ? customer.getCustomerData().getName() : "N/A");
            info.setRegisterdate(customer.getRegisterdate());
            if (unpaidPhases.isEmpty()) {
                info.setLastUnpaidPhaseNumber(null);
                info.setLateBaseDate(null);
                info.setRecentPaymentDate(null);
                info.setDaysOverdue(0L);
                info.setLateRate(0.0);
                info.setOverdueAmount(0L);
                long paidAmount = phases.stream().mapToLong(p -> p.getCharged() != null ? p.getCharged() : 0L).sum();
                info.setPaidAmount(paidAmount);
                info.setLateFee(0.0);
                info.setTotalOwed(0L);
                lateFeeInfos.add(info);
            } else {
                int lastUnpaidPhaseNumber = unpaidPhases.stream().mapToInt(Phase::getPhaseNumber).max().orElse(0);
                info.setLastUnpaidPhaseNumber(lastUnpaidPhaseNumber);
                LocalDate lateBaseDate = unpaidPhases.stream().map(Phase::getPlanneddate).min(LocalDate::compareTo).orElse(null);
                info.setLateBaseDate(lateBaseDate);
                List<Phase> paidPhases = phases.stream().filter(p -> p.getFullpaiddate() != null).collect(Collectors.toList());
                LocalDate recentPaymentDate = paidPhases.stream().map(Phase::getFullpaiddate).max(LocalDate::compareTo).orElse(null);
                info.setRecentPaymentDate(recentPaymentDate);
                long daysOverdue = lateBaseDate != null ? ChronoUnit.DAYS.between(lateBaseDate, today) : 0;
                if (daysOverdue < 0) daysOverdue = 0;
                info.setDaysOverdue(daysOverdue);
                double lateRate = 0.0005;
                info.setLateRate(lateRate);
                long overdueAmount = unpaidPhases.stream().mapToLong(p -> p.getFeesum() != null ? p.getFeesum() : 0L).sum();
                info.setOverdueAmount(overdueAmount);
                long paidAmount = phases.stream().mapToLong(p -> p.getCharged() != null ? p.getCharged() : 0L).sum();
                info.setPaidAmount(paidAmount);
                double lateFee = overdueAmount * lateRate * daysOverdue;
                info.setLateFee(lateFee);
                long totalOwed = overdueAmount + Math.round(lateFee);
                info.setTotalOwed(totalOwed);
                lateFeeInfos.add(info);
            }
        }
        return lateFeeInfos;
    }

    public List<Customer> searchCustomers(String name, String number) {
        if (name != null && number != null) {
            if (number.matches("\\d+")) {
                return customerRepository.findByNameContainingAndIdContaining(name, number);
            } else {
                return customerRepository.findByCustomerDataNameContaining(name);
            }
        } else if (name != null) {
            return customerRepository.findByCustomerDataNameContaining(name);
        } else if (number != null) {
            if (number.matches("\\d+")) {
                return customerRepository.findByIdContaining(number);
            } else {
                return Collections.emptyList();
            }
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
            return phases.stream().filter(phase -> phase.getSum() != null && phase.getSum() > 0).collect(Collectors.toList());
        } else {
            return null;
        }
    }

    public List<Phase> getCompletedPhases(Integer customerId) {
        Optional<Customer> customerOptional = customerRepository.findById(customerId);
        if (customerOptional.isPresent()) {
            Customer customer = customerOptional.get();
            List<Phase> phases = customer.getPhases();
            return phases.stream().filter(phase -> phase.getSum() == null || phase.getSum() == 0).collect(Collectors.toList());
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

    public List<CustomerDepositDTO> getAllCustomerDepositDTOs() {
        List<Customer> allCustomers = customerRepository.findAll();
        return allCustomers.stream().map(this::mapToCustomerDepositDTO).collect(Collectors.toList());
    }

    private CustomerDepositDTO mapToCustomerDepositDTO(Customer customer) {
        CustomerDepositDTO dto = new CustomerDepositDTO();
        dto.setMemberNumber(customer.getId());
        LocalDate lastPaidDate = customer.getPhases().stream().map(Phase::getFullpaiddate).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
        if (lastPaidDate != null) {
            dto.setLastTransactionDateTime(lastPaidDate.atStartOfDay());
        } else {
            dto.setLastTransactionDateTime(null);
        }
        dto.setRemarks("");
        dto.setMemo("");
        if (customer.getCustomerData() != null) {
            dto.setContractor(customer.getCustomerData().getName());
        } else {
            dto.setContractor("");
        }
        dto.setWithdrawnAmount(null);
        Long depositAmount = customer.getPhases().stream().mapToLong(p -> p.getCharged() != null ? p.getCharged() : 0L).sum();
        dto.setDepositAmount(depositAmount);
        if (customer.getFinancial() != null && customer.getFinancial().getBankname() != null) {
            dto.setBankBranch(customer.getFinancial().getBankname());
        } else {
            dto.setBankBranch("");
        }
        dto.setAccount("h");
        dto.setReservation("");
        dto.setDepositPhase1(getPhaseStatus(customer, 1));
        dto.setDepositPhase2(getPhaseStatus(customer, 2));
        dto.setDepositPhase3(getPhaseStatus(customer, 3));
        dto.setDepositPhase4(getPhaseStatus(customer, 4));
        dto.setDepositPhase5(getPhaseStatus(customer, 5));
        dto.setDepositPhase6(getPhaseStatus(customer, 6));
        dto.setDepositPhase7(getPhaseStatus(customer, 7));
        dto.setDepositPhase8(getPhaseStatus(customer, 8));
        dto.setDepositPhase9(getPhaseStatus(customer, 9));
        dto.setDepositPhase10(getPhaseStatus(customer, 10));
        if (customer.getLoan() != null) {
            dto.setLoanAmount(customer.getLoan().getLoanammount());
            dto.setLoanDate(customer.getLoan().getLoandate());
        } else {
            dto.setLoanAmount(null);
            dto.setLoanDate(null);
        }
        dto.setTemporary("");
        dto.setNote("");
        return dto;
    }

    private String getPhaseStatus(Customer customer, int phaseNumber) {
        Phase targetPhase = customer.getPhases().stream()
                .filter(p -> p.getPhaseNumber() != null && p.getPhaseNumber() == phaseNumber)
                .findFirst()
                .orElse(null);
        if (targetPhase == null) {
            return "";
        }
        Long charged = targetPhase.getCharged();
        return (charged != null && charged > 0) ? "o" : "x";
    }
}
