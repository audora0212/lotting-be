package com.audora.lotting_be.service;

import com.audora.lotting_be.model.Fee.Fee;
import com.audora.lotting_be.model.Fee.FeePerPhase;
import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.DepositHistory;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.model.customer.Status;
import com.audora.lotting_be.model.customer.minor.Loan;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    // ================================================
    // 1) 고객 생성 및 초기 Phase 설정
    // ================================================
    public Customer createCustomer(Customer customer) {
        if (customerRepository.existsById(customer.getId())) {
            throw new IllegalArgumentException("이미 존재하는 관리번호입니다.");
        }
        // Fee 조회 (groupname = type+groupname, batch = 가입차순)
        Fee fee = feeRepository.findByGroupnameAndBatch(
                customer.getType() + customer.getGroupname(),
                customer.getBatch()
        );
        // FeePerPhase 정보를 바탕으로 Phase 초기화
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

                long feesum = charge; // (service=0, exemption=0이므로)
                phase.setFeesum(feesum);

                phase.setCharged(0L);
                long discountVal = (phase.getDiscount() != null) ? phase.getDiscount() : 0L;
                phase.setSum(feesum - discountVal);

                // 예정일자 문자열을 LocalDate로 변환
                phase.setPlanneddateString(fpp.getPhasedate());
                LocalDate plannedDate = calculatePlannedDate(customer.getRegisterdate(), fpp.getPhasedate());
                phase.setPlanneddate(plannedDate);
                phase.setFullpaiddate(null);

                // 양방향 관계 설정
                phase.setCustomer(customer);
                phases.add(phase);
            }
            customer.setPhases(phases);
        }

        // Status가 없으면 새로 생성
        if (customer.getStatus() == null) {
            Status status = new Status();
            status.setCustomer(customer);
            customer.setStatus(status);
        }

        // 고객 저장 후 전체 재계산
        customer = customerRepository.save(customer);
        recalculateEverything(customer);

        return customer;
    }

    // ================================================
    // 2) 전체 재계산 (핵심 로직)
    // ================================================
    /**
     * 전체 재계산:
     * (1) 각 Phase의 charged, feesum, fullpaiddate 초기화
     * (2) 모든 DepositHistory를 순차 처리하여 입금액을 Phase에 분배
     * (3) 대출/자납 입금의 경우, 누적 금액(runningLoanPool)을 targetPhases에 배분
     * (4) 남은 일반 입금과 대출/자납 입금 잔액을 Status에 반영
     * (5) Loan 필드를 업데이트하여, 대출과 자납 금액을 별도로 누적
     * (6) 최종 저장
     */
    public void recalculateEverything(Customer customer) {
        // 1) 각 Phase 초기화
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
                phase.setSum(feesum - discountVal);
            }
        }

        // 2) 각 Phase별 누적 입금액 맵 초기화
        Map<Integer, Long> cumulativeDeposits = new HashMap<>();
        if (customer.getPhases() != null) {
            for (Phase p : customer.getPhases()) {
                cumulativeDeposits.put(p.getPhaseNumber(), 0L);
            }
        }

        // 3) DepositHistory 처리 (일반 입금과 대출/자납 입금 구분)
        List<DepositHistory> histories = customer.getDepositHistories();
        long leftoverGeneral = 0L;
        // 대출/자납 입금은 누적 변수 사용
        AtomicLong runningLoanPool = new AtomicLong(0L);
        boolean firstLoanDepositProcessed = false;

        if (histories != null && !histories.isEmpty()) {
            // 거래일시 순으로 정렬
            histories.sort(Comparator.comparing(DepositHistory::getTransactionDateTime));

            for (DepositHistory dh : histories) {
                if ("o".equalsIgnoreCase(dh.getLoanStatus())) {
                    // 대출/자납 입금 처리
                    if (!firstLoanDepositProcessed) {
                        dh.setLoanRecord("1");
                        firstLoanDepositProcessed = true;
                    } else {
                        dh.setLoanRecord("0");
                    }
                    long depositAmt = (dh.getDepositAmount() != null ? dh.getDepositAmount() : 0L);
                    // 누적 대출금 풀에 현재 입금액 추가
                    runningLoanPool.addAndGet(depositAmt);
                    // targetPhases에 따라 누적 풀에서 배분 (할인은 무시)
                    distributeLoanDepositPaymentToPhases(customer, dh, cumulativeDeposits, runningLoanPool);
                } else {
                    // 일반 입금: 기존 로직대로 처리
                    long leftover = distributeDepositPaymentToPhases(customer, dh, cumulativeDeposits);
                    leftoverGeneral += leftover;
                }
                // 변경사항 저장
                depositHistoryRepository.save(dh);
            }
        }

        // 최종 대출/자납 잔액
        long leftoverLoan = runningLoanPool.get();

        // 4) Status 업데이트
        Status st = customer.getStatus();
        if (st == null) {
            st = new Status();
            st.setCustomer(customer);
            customer.setStatus(st);
        }
        st.setExceedamount(leftoverGeneral);
        st.setLoanExceedAmount(leftoverLoan);

        // 5) Status 및 Loan 필드 업데이트
        updateStatusFields(customer);
        updateLoanField(customer);
        customerRepository.save(customer);
    }

    // ================================================
    // 3-1) 일반 입금 분배 로직 (변경 없음)
    // ================================================
    /**
     * DepositHistory (일반 입금)의 입금액을 Phase별로 배분하고,
     * 남은 금액(leftover)을 반환합니다.
     */
    public long distributeDepositPaymentToPhases(Customer customer,
                                                 DepositHistory dh,
                                                 Map<Integer, Long> cumulativeDeposits) {
        long depositAmt = (dh.getDepositAmount() != null ? dh.getDepositAmount() : 0L);
        long remaining = depositAmt;
        List<Phase> phases = customer.getPhases();
        if (phases != null) {
            phases.sort(Comparator.comparingInt(Phase::getPhaseNumber));
        }

        for (Phase phase : phases) {
            int phaseNo = phase.getPhaseNumber();
            long already = cumulativeDeposits.getOrDefault(phaseNo, 0L);
            long feesum = (phase.getFeesum() != null) ? phase.getFeesum() : 0L;
            long discount = (phase.getDiscount() != null) ? phase.getDiscount() : 0L;
            long required = (feesum - discount) - already;
            if (required <= 0) continue;
            long allocation = Math.min(remaining, required);
            if (allocation > 0) {
                boolean wasZero = (already == 0L);
                already += allocation;
                remaining -= allocation;
                phase.setCharged(already);
                if (already >= (feesum - discount)) {
                    phase.setFullpaiddate(dh.getTransactionDateTime() != null ? dh.getTransactionDateTime().toLocalDate() : null);
                }
                phase.setSum((feesum - discount) - already);
                setDepositPhaseField(dh, phaseNo, wasZero ? "1" : "0");
                cumulativeDeposits.put(phaseNo, already);
            }
            if (remaining <= 0) break;
        }
        return remaining;
    }

    // ================================================
    // 3-2) 대출/자납 입금 분배 로직
    // ================================================
    /**
     * 대출/자납 입금(depositHistory, loanStatus = 'o')의 경우,
     * 누적 대출금 풀(runningLoanPool)에 있는 금액을, depositHistory의 targetPhases에 따라 배분합니다.
     * 할인을 고려하지 않고, 해당 Phase의 feesum에서 이미 배분된 금액(cumulativeDeposits)를 제외한 금액을 배분합니다.
     * 배분 후 남은 금액은 runningLoanPool에 업데이트됩니다.
     */
    public void distributeLoanDepositPaymentToPhases(Customer customer,
                                                     DepositHistory dh,
                                                     Map<Integer, Long> cumulativeDeposits,
                                                     AtomicLong runningLoanPool) {
        long remaining = runningLoanPool.get();
        List<Phase> phases = customer.getPhases();
        if (phases != null) {
            phases.sort(Comparator.comparingInt(Phase::getPhaseNumber));
        }
        List<Integer> targetList = dh.getTargetPhases();
        if (targetList != null && !targetList.isEmpty()) {
            for (Integer phaseNo : targetList) {
                Phase phase = findPhaseByNumber(phases, phaseNo);
                if (phase == null) continue;
                long already = cumulativeDeposits.getOrDefault(phaseNo, 0L);
                long feesum = (phase.getFeesum() != null) ? phase.getFeesum() : 0L;
                long required = feesum - already; // 할인 무시
                if (required <= 0) continue;
                long allocation = Math.min(remaining, required);
                if (allocation > 0) {
                    boolean wasZero = (already == 0L);
                    already += allocation;
                    remaining -= allocation;
                    phase.setCharged(already);
                    if (already >= feesum) {
                        phase.setFullpaiddate(dh.getTransactionDateTime() != null ? dh.getTransactionDateTime().toLocalDate() : null);
                    }
                    phase.setSum(feesum - already);
                    setDepositPhaseField(dh, phaseNo, wasZero ? "1" : "0");
                    cumulativeDeposits.put(phaseNo, already);
                }
                if (remaining <= 0) break;
            }
        }
        runningLoanPool.set(remaining);
    }

    /**
     * 특정 Phase 찾기 (phaseNumber 기준)
     */
    private Phase findPhaseByNumber(List<Phase> phases, int phaseNo) {
        if (phases == null) return null;
        for (Phase p : phases) {
            if (p.getPhaseNumber() != null && p.getPhaseNumber() == phaseNo) {
                return p;
            }
        }
        return null;
    }

    /**
     * DepositPhaseN 필드 setter
     */
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
            default:
        }
    }

    // ================================================
    // 4) Status 업데이트
    // ================================================
    public void updateStatusFields(Customer customer) {
        List<Phase> phases = customer.getPhases();
        Status status = customer.getStatus();
        if (status == null) {
            status = new Status();
            status.setCustomer(customer);
            customer.setStatus(status);
        }
        if (phases != null && !phases.isEmpty()) {
            long exemptionsum = phases.stream()
                    .mapToLong(p -> (p.getExemption() != null) ? p.getExemption() : 0L)
                    .sum();
            status.setExemptionsum(exemptionsum);

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

    // ================================================
    // 5) Loan 필드 업데이트 (대출과 자납 금액 분리)
    // ================================================
    /**
     * depositHistories 중 loanStatus가 'o'인 항목에 대해,
     * 각 depositHistory의 loanDetails에서 대출금액(loanammount)과 자납금액(selfammount)을 읽어와
     * 각각 누적한 후, Loan 객체에 업데이트합니다.
     * 이를 통해, details 필드를 참조하지 않고도 대출과 자납을 별도로 처리할 수 있습니다.
     */
    public void updateLoanField(Customer customer) {
        if (customer.getDepositHistories() == null) return;

        if (customer.getLoan() == null) {
            customer.setLoan(new Loan());
        }
        Loan customerLoan = customer.getLoan();

        // 대출(loan) 금액: 각 depositHistory의 loanDetails.loanammount 누적
        long totalLoanAmount = customer.getDepositHistories().stream()
                .filter(dh -> "o".equalsIgnoreCase(dh.getLoanStatus()))
                .mapToLong(dh -> {
                    if (dh.getLoanDetails() != null && dh.getLoanDetails().getLoanammount() != null) {
                        return dh.getLoanDetails().getLoanammount();
                    }
                    return 0L;
                }).sum();
        // 자납(self) 금액: 각 depositHistory의 loanDetails.selfammount 누적
        long totalSelfAmount = customer.getDepositHistories().stream()
                .filter(dh -> "o".equalsIgnoreCase(dh.getLoanStatus()))
                .mapToLong(dh -> {
                    if (dh.getLoanDetails() != null && dh.getLoanDetails().getSelfammount() != null) {
                        return dh.getLoanDetails().getSelfammount();
                    }
                    return 0L;
                }).sum();

        customerLoan.setLoanammount(totalLoanAmount);
        customerLoan.setSelfammount(totalSelfAmount);

        // 가장 최근의 대출/자납 입금 정보를 반영
        DepositHistory mostRecentLoan = customer.getDepositHistories().stream()
                .filter(dh -> "o".equalsIgnoreCase(dh.getLoanStatus()))
                .max(Comparator.comparing(DepositHistory::getTransactionDateTime))
                .orElse(null);
        if (mostRecentLoan != null) {
            if (mostRecentLoan.getLoanDate() != null) {
                customerLoan.setLoandate(mostRecentLoan.getLoanDate());
            }
            if (mostRecentLoan.getLoanDetails() != null) {
                if (mostRecentLoan.getLoanDetails().getLoanbank() != null) {
                    customerLoan.setLoanbank(mostRecentLoan.getLoanDetails().getLoanbank());
                }
                if (mostRecentLoan.getLoanDetails().getSelfdate() != null) {
                    customerLoan.setSelfdate(mostRecentLoan.getLoanDetails().getSelfdate());
                }
            }
        }

        if (customer.getStatus() != null && customer.getStatus().getLoanExceedAmount() != null) {
            customerLoan.setLoanselfcurrent(customer.getStatus().getLoanExceedAmount());
        }

        customerRepository.save(customer);
    }

    // ================================================
    // 6) 다음 고객번호 조회
    // ================================================
    public Integer getNextCustomerId() {
        return customerRepository.getNextId();
    }

    // ================================================
    // 7) 고객 조회/저장/삭제
    // ================================================
    public Customer getCustomerById(Integer id) {
        Optional<Customer> optionalCustomer = customerRepository.findById(id);
        return optionalCustomer.orElse(null);
    }

    public Customer saveCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public void deleteCustomer(Integer id) {
        customerRepository.deleteById(id);
    }

    // ================================================
    // 8) Phase 조회: 미납/완납
    // ================================================
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

    // ================================================
    // 9) 통계: 정계약, 완납/미연체
    // ================================================
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
                            phase.getFullpaiddate() == null
            );
            return !hasOverdue;
        }).count();
    }

    // ================================================
    // 10) 연체료 정보
    // ================================================
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
                Optional<Customer> cOpt = customerRepository.findById(id);
                customers = cOpt.map(Collections::singletonList).orElse(Collections.emptyList());
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

            List<Phase> unpaidPhases = phases.stream().filter(p ->
                    p.getPlanneddate() != null &&
                            p.getPlanneddate().isBefore(today) &&
                            p.getFullpaiddate() == null
            ).collect(Collectors.toList());

            LateFeeInfo info = new LateFeeInfo();
            info.setId(c.getId());
            info.setCustomertype(c.getCustomertype() != null ? c.getCustomertype() : "N/A");
            info.setName((c.getCustomerData() != null && c.getCustomerData().getName() != null)
                    ? c.getCustomerData().getName()
                    : "N/A");
            info.setRegisterdate(c.getRegisterdate());

            if (unpaidPhases.isEmpty()) {
                info.setLastUnpaidPhaseNumber(null);
                info.setLateBaseDate(null);
                info.setRecentPaymentDate(null);
                info.setDaysOverdue(0L);
                info.setLateRate(0.0);
                info.setOverdueAmount(0L);

                long paidAmount = phases.stream()
                        .mapToLong(p -> (p.getCharged() != null) ? p.getCharged() : 0L)
                        .sum();
                info.setPaidAmount(paidAmount);

                info.setLateFee(0.0);
                info.setTotalOwed(0L);
            } else {
                int lastUnpaid = unpaidPhases.stream()
                        .mapToInt(Phase::getPhaseNumber)
                        .max().orElse(0);
                info.setLastUnpaidPhaseNumber(lastUnpaid);

                LocalDate lateBaseDate = unpaidPhases.stream()
                        .map(Phase::getPlanneddate)
                        .min(LocalDate::compareTo)
                        .orElse(null);
                info.setLateBaseDate(lateBaseDate);

                List<Phase> paidPhases = phases.stream()
                        .filter(p -> p.getFullpaiddate() != null)
                        .collect(Collectors.toList());
                LocalDate recentPaymentDate = paidPhases.stream()
                        .map(Phase::getFullpaiddate)
                        .max(LocalDate::compareTo)
                        .orElse(null);
                info.setRecentPaymentDate(recentPaymentDate);

                long daysOverdue = (lateBaseDate != null)
                        ? ChronoUnit.DAYS.between(lateBaseDate, today)
                        : 0;
                if (daysOverdue < 0) daysOverdue = 0;
                info.setDaysOverdue(daysOverdue);

                double lateRate = 0.0005;
                info.setLateRate(lateRate);

                long overdueAmount = unpaidPhases.stream()
                        .mapToLong(p -> (p.getFeesum() != null) ? p.getFeesum() : 0L)
                        .sum();
                info.setOverdueAmount(overdueAmount);

                long paidAmount = phases.stream()
                        .mapToLong(p -> (p.getCharged() != null) ? p.getCharged() : 0L)
                        .sum();
                info.setPaidAmount(paidAmount);

                double lateFee = overdueAmount * lateRate * daysOverdue;
                info.setLateFee(lateFee);

                long totalOwed = overdueAmount + Math.round(lateFee);
                info.setTotalOwed(totalOwed);
            }
            lateFeeInfos.add(info);
        }

        return lateFeeInfos;
    }

    // ================================================
    // 11) 검색
    // ================================================
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

    // ================================================
    // 12) DepositList DTO
    // ================================================
    public List<CustomerDepositDTO> getAllCustomerDepositDTOs() {
        List<Customer> allCustomers = customerRepository.findAll();
        return allCustomers.stream()
                .map(this::mapToCustomerDepositDTO)
                .collect(Collectors.toList());
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
                .mapToLong(p -> (p.getCharged() != null) ? p.getCharged() : 0L)
                .sum();
        dto.setDepositAmount(depositAmount);

        dto.setBankBranch(
                (customer.getFinancial() != null && customer.getFinancial().getBankname() != null)
                        ? customer.getFinancial().getBankname()
                        : ""
        );
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
                    .filter(dh -> "o".equalsIgnoreCase(dh.getLoanStatus()))
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

    /**
     * 1~10차 입금 상태: charged > 0 ? "o" : "x"
     */
    private String getPhaseStatus(Customer customer, int phaseNumber) {
        if (customer.getPhases() == null) return "";
        Phase targetPhase = customer.getPhases().stream()
                .filter(p -> p.getPhaseNumber() != null && p.getPhaseNumber() == phaseNumber)
                .findFirst()
                .orElse(null);
        if (targetPhase == null) return "";
        Long charged = targetPhase.getCharged();
        return (charged != null && charged > 0) ? "o" : "x";
    }

    // ================================================
    // 13) 도우미: plannedDate 계산
    // ================================================
    /**
     * 예: "3달" -> registerDate.plusMonths(3)
     *     "1년" -> registerDate.plusYears(1)
     */
    private LocalDate calculatePlannedDate(LocalDate registerDate, String phasedate) {
        if (registerDate == null) {
            registerDate = LocalDate.now();
        }
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
}
