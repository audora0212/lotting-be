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
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
public class CustomerService {
    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private FeeRepository feeRepository;
    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    // ================================================
    // 1) 고객 생성 및 초기 Phase 설정
    // ================================================
    @Transactional
    public Customer createCustomer(Customer customer, boolean recalc) {

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
     * (2) 모든 DepositHistory를 순차 처리하여 입금액을 Phase에 배분
     *     단, 해당 DepositHistory의 depositPhase 필드에 예상치 못한 값이 있으면
     *     해당 기록은 배분 계산에서 제외합니다.
     * (3) 대출/자납 입금의 경우, 누적 금액(runningLoanPool)을 targetPhases에 배분하고,
     *     동시에 해당 거래가 대출, 자납, 또는 둘 다인지를 loan_details 값을 통해 판단하여,
     *     대출이면 loan_record, 자납이면 self_record를 첫 거래에 "1", 그 이후에 "0"으로 기록합니다.
     * (4) 남은 일반 입금과 대출/자납 입금 잔액을 Status에 반영
     * (5) Loan 필드를 업데이트하여, 대출과 자납 금액을 별도로 누적
     * (6) 최종 저장
     */
// 기존 recalculateEverything 메서드의 수정 버전 (대출/자납 입금기록은 수동 Loan 값을 소진하는 로직)
// CustomerService.java


    public void recalculateEverything(Customer customer) {
        // 1) 각 Phase 초기화 (일반 입금용)
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

        // 2) 일반 입금용 Phase별 누적 입금액 초기화
        Map<Integer, Long> cumulativeDeposits = new HashMap<>();
        if (customer.getPhases() != null) {
            for (Phase p : customer.getPhases()) {
                cumulativeDeposits.put(p.getPhaseNumber(), 0L);
            }
        }

        long leftoverGeneral = 0L;
        long loanConsumedSum = 0L; // 대출/자납 입금 기록에서 실제 배분된 금액 누적 변수
        int countLoanRecords = 0;  // 지금까지 처리한 대출 입금 기록 수
        int countSelfRecords = 0;  // 지금까지 처리한 자납 입금 기록 수

        List<DepositHistory> histories = customer.getDepositHistories();
        if (histories != null && !histories.isEmpty()) {
            // 거래일시 순으로 정렬
            histories.sort(Comparator.comparing(DepositHistory::getTransactionDateTime));
            for (DepositHistory dh : histories) {
                if (!"o".equalsIgnoreCase(dh.getLoanStatus())) {
                    // 일반 입금 기록: 기존 분배 로직 사용
                    long leftover = distributeDepositPaymentToPhases(customer, dh, cumulativeDeposits);
                    leftoverGeneral += leftover;
                    depositHistoryRepository.save(dh);
                } else {
                    // 대출/자납 입금 기록:
                    long depositAmt = (dh.getDepositAmount() != null ? dh.getDepositAmount() : 0L);
                    AtomicLong localLoanPool = new AtomicLong(depositAmt);
                    List<Integer> targetList = dh.getTargetPhases();
                    // 신규 메서드를 호출하여 각 phase별 배분액을 수집합니다.
                    Map<Integer, Long> allocationForThisRecord = new HashMap<>();
                    if (targetList != null && !targetList.isEmpty()) {
                        allocationForThisRecord = distributeLoanDepositPaymentToPhasesAndCollectAllocation(customer, dh, cumulativeDeposits, localLoanPool);
                    }

                    // loan_record, self_record 설정
                    boolean hasLoanValue = (dh.getLoanDetails() != null &&
                            dh.getLoanDetails().getLoanammount() != null &&
                            dh.getLoanDetails().getLoanammount() > 0);
                    boolean hasSelfValue = (dh.getLoanDetails() != null &&
                            dh.getLoanDetails().getSelfammount() != null &&
                            dh.getLoanDetails().getSelfammount() > 0);
                    if (hasLoanValue) {
                        dh.setLoanRecord(countLoanRecords == 0 ? "1" : "0");
                        countLoanRecords++;
                    } else {
                        dh.setLoanRecord(null);
                    }
                    if (hasSelfValue) {
                        dh.setSelfRecord(countSelfRecords == 0 ? "1" : "0");
                        countSelfRecords++;
                    } else {
                        dh.setSelfRecord(null);
                    }
                    depositHistoryRepository.save(dh);

                    // 실제 사용된 금액 = depositAmt - localLoanPool.get()
                    long usedAmount = depositAmt - localLoanPool.get();
                    loanConsumedSum += usedAmount;

                    // 신규: 배분된 결과를 JSON 형식으로 작성
                    StringBuilder allocationDetailJson = new StringBuilder("{");
                    for (Integer phaseNo : allocationForThisRecord.keySet()) {
                        Phase phase = findPhaseByNumber(customer.getPhases(), phaseNo);
                        if (phase != null) {
                            long allocated = allocationForThisRecord.get(phaseNo);
                            long required = (phase.getFeesum() != null ? phase.getFeesum() : 0L)
                                    - (phase.getDiscount() != null ? phase.getDiscount() : 0L);
                            // remainingNeeded는 phase에 현재 누적된 charged값(재계산 후)에서 계산
                            long remainingNeeded = required - (phase.getCharged() != null ? phase.getCharged() : 0L);
                            if (remainingNeeded < 0) remainingNeeded = 0;
                            allocationDetailJson.append("\"phase").append(phaseNo).append("\":")
                                    .append("{\"allocated\":").append(allocated)
                                    .append(",\"remainingNeeded\":").append(remainingNeeded)
                                    .append("},");
                        }
                    }
                    if (allocationDetailJson.charAt(allocationDetailJson.length() - 1) == ',') {
                        allocationDetailJson.deleteCharAt(allocationDetailJson.length() - 1);
                    }
                    allocationDetailJson.append("}");
                    dh.setAllocationDetail(allocationDetailJson.toString());
                }
            }
        }

        // 4) Status 업데이트
        Status st = customer.getStatus();
        if (st == null) {
            st = new Status();
            st.setCustomer(customer);
            customer.setStatus(st);
        }
        st.setExceedamount(leftoverGeneral);
        // 대출/자납 잔액: 수동 입력된 Loan 값(대출액 + 자납액)에서 대출/자납 입금 중 실제 배분된 금액을 차감
        long manualLoanTotal = 0L;
        if (customer.getLoan() != null) {
            manualLoanTotal = (customer.getLoan().getLoanammount() != null ? customer.getLoan().getLoanammount() : 0L)
                    + (customer.getLoan().getSelfammount() != null ? customer.getLoan().getSelfammount() : 0L);
        }
        st.setLoanExceedAmount(Math.max(0, manualLoanTotal - loanConsumedSum));
        updateStatusFields(customer);
        updateLoanField(customer);
        customerRepository.save(customer);
    }

    private Map<Integer, Long> distributeLoanDepositPaymentToPhasesAndCollectAllocation(Customer customer,
                                                                                        DepositHistory dh,
                                                                                        Map<Integer, Long> cumulativeDeposits,
                                                                                        AtomicLong runningLoanPool) {
        Map<Integer, Long> allocationMap = new HashMap<>();
        List<Integer> targetList = dh.getTargetPhases();
        List<Phase> phases = customer.getPhases();
        if (phases != null) {
            phases.sort(Comparator.comparingInt(Phase::getPhaseNumber));
        }
        if (targetList != null) {
            for (Integer phaseNo : targetList) {
                Phase phase = findPhaseByNumber(phases, phaseNo);
                if (phase == null) continue;
                long already = cumulativeDeposits.getOrDefault(phaseNo, 0L);
                long feesum = (phase.getFeesum() != null) ? phase.getFeesum() : 0L;
                // 할인액은 무시: required = feesum - already;
                long required = feesum - already;
                if (required <= 0) continue;
                long allocation = Math.min(runningLoanPool.get(), required);
                if (allocation > 0) {
                    boolean wasZero = (already == 0L);
                    already += allocation;
                    runningLoanPool.set(runningLoanPool.get() - allocation);
                    phase.setCharged(already);
                    if (already >= feesum) {
                        if (dh.getTransactionDateTime() != null) {
                            phase.setFullpaiddate(dh.getTransactionDateTime().toLocalDate());
                        }
                    }
                    phase.setSum(feesum - already);
                    setDepositPhaseField(dh, phaseNo, wasZero ? "1" : "0");
                    cumulativeDeposits.put(phaseNo, already);
                    allocationMap.put(phaseNo, allocation);
                }
                if (runningLoanPool.get() <= 0) break;
            }
        }
        return allocationMap;
    }




    // 기존 distributeLoanDepositPaymentToPhases 메서드 (변경 없이 사용)
    public void distributeLoanDepositPaymentToPhases(Customer customer,
                                                     DepositHistory dh,
                                                     Map<Integer, Long> cumulativeDeposits,
                                                     AtomicLong runningLoanPool) {
        String dp1 = dh.getDepositPhase1();
        if (dp1 != null && !dp1.trim().isEmpty() && !( "0".equals(dp1) || "1".equals(dp1) || "2".equals(dp1) )) {
            return;
        }
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
                long required = feesum - already;
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

    // updateLoanField 메서드는 중복 없이 하나만 존재하도록 함.
    public void updateLoanField(Customer customer) {
        if (customer.getLoan() == null) {
            customer.setLoan(new Loan());
        }
        // 수동 입력 Loan 값은 재계산 없이 그대로 유지합니다.
        customerRepository.save(customer);
    }


    // ================================================
    // 3-1) 일반 입금 분배 로직
    // ================================================
    /**
     * DepositHistory(일반 입금)의 입금액을 Phase별로 배분하고 남은 금액(leftover)을 반환합니다.
     * 단, 해당 DepositHistory의 depositPhase1에 예상치 못한 값이 있으면,
     * 배분을 전혀 수행하지 않고 depositAmount 전체를 남김으로 처리합니다.
     */
    public long distributeDepositPaymentToPhases(Customer customer, DepositHistory dh, Map<Integer, Long> cumulativeDeposits) {
        // depositPhase1 값 전처리: 공백 제거, 소문자 변환
        String dp1 = dh.getDepositPhase1();
        if (dp1 != null) {
            dp1 = dp1.trim().toLowerCase();
        }
        // depositPhase1이 "", "0", "1", "2"가 아니면(예: "x") 해당 입금액을 배분하지 않음
        if (dp1 != null && !(dp1.isEmpty() || dp1.equals("0") || dp1.equals("1") || dp1.equals("2"))) {
            logger.info("depositPhase1 값이 '{}' 이므로 해당 입금은 phase 분배에 반영되지 않습니다.", dp1);
            // depositAmount 전체를 leftover로 반환하는 대신 0L 반환하여 아예 배분하지 않음
            return 0L;
        }

        // 정상적인 배분 로직 진행
        long depositAmt = (dh.getDepositAmount() != null ? dh.getDepositAmount() : 0L);
        long remaining = depositAmt;
        List<Phase> phases = customer.getPhases();
        if (phases != null) {
            phases.sort(Comparator.comparingInt(Phase::getPhaseNumber));
        }
        for (Phase phase : phases) {
            int phaseNo = phase.getPhaseNumber();
            long already = cumulativeDeposits.getOrDefault(phaseNo, 0L);
            long feesum = (phase.getFeesum() != null ? phase.getFeesum() : 0L);
            long discount = (phase.getDiscount() != null ? phase.getDiscount() : 0L);
            long required = (feesum - discount) - already;
            if (required <= 0) continue;
            long allocation = Math.min(remaining, required);
            if (allocation > 0) {
                boolean wasZero = (already == 0L);
                already += allocation;
                remaining -= allocation;
                phase.setCharged(already);
                if (already >= (feesum - discount)) {
                    if (dh.getTransactionDateTime() != null) {
                        phase.setFullpaiddate(dh.getTransactionDateTime().toLocalDate());
                    }
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
     * 대출/자납 입금(loanStatus = 'o')의 경우,
     * 누적 대출금 풀(runningLoanPool)의 금액을, DepositHistory의 targetPhases에 따라 배분합니다.
     * 단, depositPhase1에 예상치 못한 값이 있으면 배분 계산을 전혀 수행하지 않습니다.
     */


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

    // ================================================
    // 3-3) depositPhase 필드 설정
    // 만약 depositPhase 필드에 이미 값(예상치 못한 값)이 있다면, 이를 덮어쓰지 않고 그대로 유지합니다.
    // ================================================
    private void setDepositPhaseField(DepositHistory dh, int phaseNo, String computedValue) {
        String currentValue = null;
        switch (phaseNo) {
            case 1: currentValue = dh.getDepositPhase1(); break;
            case 2: currentValue = dh.getDepositPhase2(); break;
            case 3: currentValue = dh.getDepositPhase3(); break;
            case 4: currentValue = dh.getDepositPhase4(); break;
            case 5: currentValue = dh.getDepositPhase5(); break;
            case 6: currentValue = dh.getDepositPhase6(); break;
            case 7: currentValue = dh.getDepositPhase7(); break;
            case 8: currentValue = dh.getDepositPhase8(); break;
            case 9: currentValue = dh.getDepositPhase9(); break;
            case 10: currentValue = dh.getDepositPhase10(); break;
            default:
                break;
        }
        // 만약 현재 값이 이미 존재하고 허용된 값("0", "1", "2")이 아니라면, 아무것도 변경하지 않습니다.
        if (currentValue != null && !( "0".equals(currentValue) || "1".equals(currentValue) || "2".equals(currentValue) )) {
            System.out.println("Phase " + phaseNo + " depositPhase already has a record value (" + currentValue + "); computed value (" + computedValue + ") will not override it.");
            return;
        }
        // 그렇지 않으면 computedValue로 설정
        switch (phaseNo) {
            case 1: dh.setDepositPhase1(computedValue); break;
            case 2: dh.setDepositPhase2(computedValue); break;
            case 3: dh.setDepositPhase3(computedValue); break;
            case 4: dh.setDepositPhase4(computedValue); break;
            case 5: dh.setDepositPhase5(computedValue); break;
            case 6: dh.setDepositPhase6(computedValue); break;
            case 7: dh.setDepositPhase7(computedValue); break;
            case 8: dh.setDepositPhase8(computedValue); break;
            case 9: dh.setDepositPhase9(computedValue); break;
            case 10: dh.setDepositPhase10(computedValue); break;
            default:
                break;
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
     * DepositHistory 중 loanStatus가 'o'인 항목에 대해,
     * 각 DepositHistory의 loanDetails에서 대출금액(loanammount)과 자납금액(selfammount)을 누적하여
     * Loan 객체를 업데이트합니다.
     */


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
        return customerRepository.count();
    }

    public long countFullyPaidCustomers() {
        List<Customer> allCustomers = customerRepository.findAll();
        return allCustomers.stream().filter(customer -> {
            Status status = customer.getStatus();
            return status != null
                    && status.getAmmountsum() != null && status.getAmmountsum() != 0
                    && status.getUnpaidammout() != null && status.getUnpaidammout() == 0;
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
                    ? c.getCustomerData().getName() : "N/A");
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
                long daysOverdue = (lateBaseDate != null) ? ChronoUnit.DAYS.between(lateBaseDate, today) : 0;
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
        List<Customer> customers;
        if (name != null && number != null) {
            if (number.matches("\\d+")) {
                customers = customerRepository.findByNameContainingAndIdContaining(name, number);
            } else {
                customers = customerRepository.findByCustomerDataNameContaining(name);
            }
        } else if (name != null) {
            customers = customerRepository.findByCustomerDataNameContaining(name);
        } else if (number != null) {
            if (number.matches("\\d+")) {
                customers = customerRepository.findByIdContaining(number);
            } else {
                customers = Collections.emptyList();
            }
        } else {
            // 파라미터가 없으면 모든 고객을 반환
            customers = customerRepository.findAll();
        }
        // id가 1인 더미 데이터는 필터링
        return customers.stream()
                .filter(customer -> !customer.getId().equals(1))
                .collect(Collectors.toList());
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
        dto.setBankBranch((customer.getFinancial() != null && customer.getFinancial().getBankname() != null)
                ? customer.getFinancial().getBankname() : "");
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
     * 예: "3달" -> registerDate.plusMonths(3), "1년" -> registerDate.plusYears(1)
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
    @Transactional
    public List<Customer> getAllCustomersWithPhases() {
        List<Customer> customers = customerRepository.findAll();
        for (Customer customer : customers) {
            // phases 컬렉션 강제 초기화
            if (customer.getPhases() != null) {
                customer.getPhases().size();
            }
        }
        return customers;
    }


}
