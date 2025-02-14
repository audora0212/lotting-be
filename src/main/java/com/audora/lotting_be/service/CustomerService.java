package com.audora.lotting_be.service;

import com.audora.lotting_be.model.Fee.Fee;
import com.audora.lotting_be.model.Fee.FeePerPhase;
import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.DepositHistory;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.model.customer.Status;
import com.audora.lotting_be.payload.response.CustomerDepositDTO;
import com.audora.lotting_be.payload.response.LateFeeInfo;
import com.audora.lotting_be.repository.CustomerRepository;
import com.audora.lotting_be.repository.DepositHistoryRepository;
import com.audora.lotting_be.repository.FeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    // DepositHistory 업데이트용 Repository (변경 사항을 DB에 반영)
    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    // ======================== 1) 고객 생성 및 초기 Phase 설정 ========================
    public Customer createCustomer(Customer customer) {
        if (customerRepository.existsById(customer.getId())) {
            throw new IllegalArgumentException("이미 존재하는 관리번호입니다.");
        }
        // Fee 조회 및 phase 초기화
        Fee fee = feeRepository.findByGroupnameAndBatch(
                customer.getType() + customer.getGroupname(),
                customer.getBatch()
        );
        if (fee != null) {
            List<FeePerPhase> feePerPhases = fee.getFeePerPhases();
            List<Phase> phases = new ArrayList<>();
            for (FeePerPhase fpp : feePerPhases) {
                Phase phase = new Phase();
                phase.setPhaseNumber(fpp.getPhaseNumber());
                long charge = (fpp.getPhasefee() != null) ? fpp.getPhasefee() : 0L;
                phase.setCharge(charge);
                phase.setService(0L);
                phase.setExemption(0L);
                long feesum = charge + 0L - 0L;
                phase.setFeesum(feesum);
                phase.setCharged(0L);
                long discountVal = (phase.getDiscount() != null) ? phase.getDiscount() : 0L;
                // 일반 입금 기준: net due = feesum - discount
                phase.setSum(feesum - discountVal);
                phase.setPlanneddateString(fpp.getPhasedate());
                LocalDate plannedDate = calculatePlannedDate(customer.getRegisterdate(), fpp.getPhasedate());
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
        customer = customerRepository.save(customer);
        recalculateEverything(customer);
        return customer;
    }

    // ======================== 2) 전체 재계산 ========================
    public void recalculateEverything(Customer customer) {
        // 1) 각 phase의 입금 관련 필드 초기화
        if (customer.getPhases() != null) {
            for (Phase phase : customer.getPhases()) {
                phase.setCharged(0L);
                phase.setFullpaiddate(null);
                long charge = (phase.getCharge() != null) ? phase.getCharge() : 0L;
                long service = (phase.getService() != null) ? phase.getService() : 0L;
                long exemption = (phase.getExemption() != null) ? phase.getExemption() : 0L;
                long feesum = charge + service - exemption;
                phase.setFeesum(feesum);
                long discountVal = (phase.getDiscount() != null) ? phase.getDiscount() : 0L;
                phase.setSum(feesum - discountVal); // 일반 입금 기준
            }
        }
        // 2) phase별 누적 입금액을 기록할 맵 초기화
        Map<Integer, Long> cumulativeDeposits = new HashMap<>();
        if (customer.getPhases() != null) {
            for (Phase phase : customer.getPhases()) {
                cumulativeDeposits.put(phase.getPhaseNumber(), 0L);
            }
        }
        // 3) DepositHistory 기록들을 시간순으로 처리
        List<DepositHistory> histories = customer.getDepositHistories();
        long leftoverGeneral = 0L;
        long leftoverLoan = 0L;
        boolean firstLoanDepositProcessed = false;
        if (histories != null && !histories.isEmpty()) {
            histories.sort(Comparator.comparing(DepositHistory::getTransactionDateTime));
            for (DepositHistory dh : histories) {
                boolean isLoanDeposit = (dh.getLoanStatus() != null && !dh.getLoanStatus().trim().isEmpty());
                // 대출/자납 입금인 경우, 첫 기록이면 loanStatus를 "1", 그 이후는 "0"
                if (isLoanDeposit) {
                    if (!firstLoanDepositProcessed) {
                        dh.setLoanStatus("1");
                        firstLoanDepositProcessed = true;
                    } else {
                        dh.setLoanStatus("0");
                    }
                }
                long leftover = distributeDepositPaymentToPhases(customer, dh, cumulativeDeposits);
                if (isLoanDeposit) {
                    leftoverLoan += leftover;
                } else {
                    leftoverGeneral += leftover;
                }
                // 변경된 depositHistory 레코드를 DB에 저장
                depositHistoryRepository.save(dh);
            }
        }
        // 4) 남은 금액 저장: 일반 입금 leftover -> exceedamount, 대출/자납 leftover -> loanExceedAmount
        Status st = customer.getStatus();
        if (st == null) {
            st = new Status();
            st.setCustomer(customer);
            customer.setStatus(st);
        }
        st.setExceedamount(leftoverGeneral);
        st.setLoanExceedAmount(leftoverLoan);
        // 5) 상태 업데이트 (미납금 등)
        updateStatusFields(customer);
        // 6) 대출/자납 입금 기록 기반으로 Loan 필드 업데이트 (customer.minor.Loan)
        updateLoanField(customer);
        // 7) 최종 DB 저장
        customerRepository.save(customer);
    }

    /**
     * 하나의 DepositHistory 레코드를 처리하여 phase별로 입금액을 분배하고,
     * depositHistory의 depositPhase1~10 필드를 업데이트하는 메서드.
     *
     * - 일반 입금 (isLoanDeposit == false): required = (feesum - discount) - cumulativeDeposit
     * - 대출/자납 입금 (isLoanDeposit == true): depositHistory에서 "o"로 지정된 phase에 대해
     *      required = feesum - cumulativeDeposit  (할인액 무시)
     *
     * 입금액이 phase로 할당되는 순간, 해당 depositHistory의 해당 phase의 depositPhase 필드를,
     * 해당 phase의 누적 입금이 처음이면 "1", 그렇지 않으면 "0"으로 업데이트합니다.
     *
     * @return 해당 DepositHistory 레코드에서 분배 후 남은 금액
     */
    public long distributeDepositPaymentToPhases(Customer customer, DepositHistory dh, Map<Integer, Long> cumulativeDeposits) {
        long remaining = (dh.getDepositAmount() != null ? dh.getDepositAmount() : 0L);
        List<Phase> phases = customer.getPhases();
        phases.sort(Comparator.comparingInt(Phase::getPhaseNumber));
        boolean isLoanDeposit = (dh.getLoanStatus() != null && !dh.getLoanStatus().trim().isEmpty());
        for (Phase phase : phases) {
            int phaseNo = phase.getPhaseNumber();
            // 대출/자납 입금인 경우, depositHistory에서 지정한 phase에 "o"가 있는 경우만 처리
            if (isLoanDeposit) {
                String indicator = getDepositPhaseField(dh, phaseNo);
                if (!"o".equalsIgnoreCase(indicator)) {
                    continue;
                }
            }
            long already = cumulativeDeposits.getOrDefault(phaseNo, 0L);
            long feesum = (phase.getFeesum() != null ? phase.getFeesum() : 0L);
            long discount = (phase.getDiscount() != null ? phase.getDiscount() : 0L);
            long required = isLoanDeposit ? (feesum - already) : ((feesum - discount) - already);
            if (required <= 0) continue;
            long allocation = Math.min(remaining, required);
            if (allocation > 0) {
                already += allocation;
                remaining -= allocation;
                phase.setCharged(already);
                // fullpaiddate는 해당 phase에 할당된 금액이 feesum(대출/자납의 경우) 또는 (feesum-discount)(일반 입금의 경우)와 같으면 업데이트
                if (isLoanDeposit) {
                    if (already >= feesum) {
                        phase.setFullpaiddate(dh.getTransactionDateTime() != null ? dh.getTransactionDateTime().toLocalDate() : null);
                    }
                } else {
                    if (already >= (feesum - discount)) {
                        phase.setFullpaiddate(dh.getTransactionDateTime() != null ? dh.getTransactionDateTime().toLocalDate() : null);
                    }
                }
                phase.setSum(isLoanDeposit ? (feesum - already) : ((feesum - discount) - already));
                // depositPhase 업데이트: 할당이 발생한 경우, 만약 이전에 해당 phase에 할당이 없었다면 "1", 아니면 "0"
                String valueToSet = (cumulativeDeposits.get(phaseNo) == 0L) ? "1" : "0";
                setDepositPhaseField(dh, phaseNo, valueToSet);
                cumulativeDeposits.put(phaseNo, already);
            }
            if (remaining <= 0) break;
        }
        return remaining;
    }

    /**
     * DepositHistory 중 대출/자납 입금(loanStatus ≠ 공란) 레코드들의 depositAmount 합과,
     * 가장 최근 레코드의 loanDate 및 loanDetails.loanbank(존재할 경우)를
     * 고객의 Loan 필드(기록용, customer.minor.Loan)에 업데이트합니다.
     */
    public void updateLoanField(Customer customer) {
        if (customer.getDepositHistories() != null && customer.getLoan() != null) {
            long totalLoan = customer.getDepositHistories().stream()
                    .filter(dh -> dh.getLoanStatus() != null && !dh.getLoanStatus().trim().isEmpty())
                    .mapToLong(dh -> (dh.getDepositAmount() != null ? dh.getDepositAmount() : 0L))
                    .sum();
            customer.getLoan().setLoanammount(totalLoan);
            DepositHistory mostRecentLoan = customer.getDepositHistories().stream()
                    .filter(dh -> dh.getLoanStatus() != null && !dh.getLoanStatus().trim().isEmpty())
                    .max(Comparator.comparing(DepositHistory::getTransactionDateTime))
                    .orElse(null);
            if (mostRecentLoan != null) {
                customer.getLoan().setLoandate(mostRecentLoan.getLoanDate());
                // 만약 mostRecentLoan의 loanDetails가 존재하면, loanbank를 업데이트
                if (mostRecentLoan.getLoanDetails() != null && mostRecentLoan.getLoanDetails().getLoanbank() != null) {
                    customer.getLoan().setLoanbank(mostRecentLoan.getLoanDetails().getLoanbank());
                }
                // 필요에 따라 self납 관련 필드도 업데이트할 수 있음.
            }
        }
    }

    public void updateStatusFields(Customer customer) {
        List<Phase> phases = customer.getPhases();
        Status status = customer.getStatus();
        if (status == null) {
            status = new Status();
            status.setCustomer(customer);
            customer.setStatus(status);
        }
        if (phases != null) {
            long exemptionsum = phases.stream()
                    .mapToLong(p -> (p.getExemption() != null) ? p.getExemption() : 0L)
                    .sum();
            status.setExemptionsum(exemptionsum);
            // 일반 입금 기준 미납액 = (feesum - discount) - charged
            long unpaidAmmout = phases.stream().mapToLong(p -> {
                long feesum = (p.getFeesum() != null) ? p.getFeesum() : 0L;
                long discount = (p.getDiscount() != null) ? p.getDiscount() : 0L;
                long depositPaid = (p.getCharged() != null) ? p.getCharged() : 0L;
                return ((feesum - discount) - depositPaid);
            }).sum();
            status.setUnpaidammout(unpaidAmmout);
            LocalDate today = LocalDate.now();
            List<Integer> unpaidPhases = phases.stream()
                    .filter(p -> p.getPlanneddate() != null &&
                            p.getPlanneddate().isBefore(today) &&
                            p.getFullpaiddate() == null)
                    .map(Phase::getPhaseNumber)
                    .sorted()
                    .collect(Collectors.toList());
            String unpaidPhaseStr = unpaidPhases.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            status.setUnpaidphase(unpaidPhaseStr);
            long ammountsum = phases.stream()
                    .mapToLong(p -> (p.getFeesum() != null) ? p.getFeesum() : 0L)
                    .sum();
            status.setAmmountsum(ammountsum);
            status.setPercent40((long) (ammountsum * 0.4));
        }
    }

    // 도우미 메서드: depositPhase 필드 getter/setter (phase 번호 1~10)
    private String getDepositPhaseField(DepositHistory dh, int phaseNo) {
        switch (phaseNo) {
            case 1: return dh.getDepositPhase1();
            case 2: return dh.getDepositPhase2();
            case 3: return dh.getDepositPhase3();
            case 4: return dh.getDepositPhase4();
            case 5: return dh.getDepositPhase5();
            case 6: return dh.getDepositPhase6();
            case 7: return dh.getDepositPhase7();
            case 8: return dh.getDepositPhase8();
            case 9: return dh.getDepositPhase9();
            case 10: return dh.getDepositPhase10();
            default: return null;
        }
    }

    private void setDepositPhaseField(DepositHistory dh, int phaseNo, String value) {
        switch (phaseNo) {
            case 1: dh.setDepositPhase1(value); break;
            case 2: dh.setDepositPhase2(value); break;
            case 3: dh.setDepositPhase3(value); break;
            case 4: dh.setDepositPhase4(value); break;
            case 5: dh.setDepositPhase5(value); break;
            case 6: dh.setDepositPhase6(value); break;
            case 7: dh.setDepositPhase7(value); break;
            case 8: dh.setDepositPhase8(value); break;
            case 9: dh.setDepositPhase9(value); break;
            case 10: dh.setDepositPhase10(value); break;
        }
    }

    public Integer getNextCustomerId() {
        return customerRepository.getNextId();
    }

    public Customer getCustomerById(Integer id) {
        Optional<Customer> optionalCustomer = customerRepository.findById(id);
        return optionalCustomer.orElse(null);
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
        }
        return false;
    }

    public List<Phase> getPendingPhases(Integer customerId) {
        Optional<Customer> customerOptional = customerRepository.findById(customerId);
        if (customerOptional.isPresent()) {
            Customer customer = customerOptional.get();
            List<Phase> phases = customer.getPhases();
            return phases.stream()
                    .filter(phase -> phase.getSum() != null && phase.getSum() > 0)
                    .collect(Collectors.toList());
        }
        return null;
    }

    public List<Phase> getCompletedPhases(Integer customerId) {
        Optional<Customer> customerOptional = customerRepository.findById(customerId);
        if (customerOptional.isPresent()) {
            Customer customer = customerOptional.get();
            List<Phase> phases = customer.getPhases();
            return phases.stream()
                    .filter(phase -> phase.getSum() == null || phase.getSum() == 0)
                    .collect(Collectors.toList());
        }
        return null;
    }

    public long countContractedCustomers() {
        return customerRepository.countByCustomertype("c");
    }

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
        for (Customer c : customers) {
            List<Phase> phases = c.getPhases();
            if (phases == null || phases.isEmpty()) continue;
            List<Phase> unpaidPhases = phases.stream().filter(phase ->
                            phase.getPlanneddate() != null &&
                                    phase.getPlanneddate().isBefore(today) &&
                                    phase.getFullpaiddate() == null)
                    .collect(Collectors.toList());
            LateFeeInfo info = new LateFeeInfo();
            info.setId(c.getId());
            info.setCustomertype(c.getCustomertype() != null ? c.getCustomertype() : "N/A");
            info.setName((c.getCustomerData() != null && c.getCustomerData().getName() != null)
                    ? c.getCustomerData().getName() : "N/A");
            info.setRegisterdate(c.getRegisterdate());
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
                int lastUnpaid = unpaidPhases.stream().mapToInt(Phase::getPhaseNumber).max().orElse(0);
                info.setLastUnpaidPhaseNumber(lastUnpaid);
                LocalDate lateBaseDate = unpaidPhases.stream().map(Phase::getPlanneddate).min(LocalDate::compareTo).orElse(null);
                info.setLateBaseDate(lateBaseDate);
                List<Phase> paidPhases = phases.stream().filter(p -> p.getFullpaiddate() != null).collect(Collectors.toList());
                LocalDate recentPaymentDate = paidPhases.stream().map(Phase::getFullpaiddate).max(LocalDate::compareTo).orElse(null);
                info.setRecentPaymentDate(recentPaymentDate);
                long daysOverdue = (lateBaseDate != null) ? ChronoUnit.DAYS.between(lateBaseDate, today) : 0;
                if (daysOverdue < 0) daysOverdue = 0;
                info.setDaysOverdue(daysOverdue);
                double lateRate = 0.0005;
                info.setLateRate(lateRate);
                long overdueAmount = unpaidPhases.stream()
                        .mapToLong(p -> (p.getFeesum() != null) ? p.getFeesum() : 0L)
                        .sum();
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

    public List<CustomerDepositDTO> getAllCustomerDepositDTOs() {
        List<Customer> allCustomers = customerRepository.findAll();
        return allCustomers.stream().map(this::mapToCustomerDepositDTO).collect(Collectors.toList());
    }

    private CustomerDepositDTO mapToCustomerDepositDTO(Customer customer) {
        CustomerDepositDTO dto = new CustomerDepositDTO();
        dto.setMemberNumber(customer.getId());
        LocalDate lastPaidDate = customer.getPhases().stream()
                .map(Phase::getFullpaiddate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        dto.setLastTransactionDateTime(lastPaidDate != null ? lastPaidDate.atStartOfDay() : null);
        dto.setRemarks("");
        dto.setMemo("");
        dto.setContractor(customer.getCustomerData() != null ? customer.getCustomerData().getName() : "");
        dto.setWithdrawnAmount(null);
        Long depositAmount = customer.getPhases().stream()
                .mapToLong(p -> p.getCharged() != null ? p.getCharged() : 0L)
                .sum();
        dto.setDepositAmount(depositAmount);
        dto.setBankBranch(customer.getFinancial() != null && customer.getFinancial().getBankname() != null ?
                customer.getFinancial().getBankname() : "");
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
        DepositHistory loanDeposit = null;
        if (customer.getDepositHistories() != null) {
            loanDeposit = customer.getDepositHistories().stream()
                    .filter(dh -> dh.getLoanStatus() != null && !dh.getLoanStatus().trim().isEmpty())
                    .max(Comparator.comparing(DepositHistory::getTransactionDateTime))
                    .orElse(null);
        }
        if (loanDeposit != null) {
            dto.setLoanAmount(loanDeposit.getDepositAmount());
            dto.setLoanDate(loanDeposit.getLoanDate());
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
        if (targetPhase == null) return "";
        Long charged = targetPhase.getCharged();
        return (charged != null && charged > 0) ? "o" : "x";
    }

    private LocalDate calculatePlannedDate(LocalDate registerDate, String phasedate) {
        if (registerDate == null) registerDate = LocalDate.now();
        if (phasedate == null || phasedate.isEmpty()) return registerDate;
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

    public void deleteCustomer(Integer id) {
        customerRepository.deleteById(id);
    }
}
