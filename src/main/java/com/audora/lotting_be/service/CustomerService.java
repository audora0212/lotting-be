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
        // 1) Fee 조회 (groupname= type+groupname, batch= 가입차순)
        Fee fee = feeRepository.findByGroupnameAndBatch(
                customer.getType() + customer.getGroupname(),
                customer.getBatch()
        );
        // 2) FeePerPhase -> Phase 초기화
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

                // FeePerPhase에 기재된 예정일자 문자열 → 실제 LocalDate 변환
                phase.setPlanneddateString(fpp.getPhasedate());
                LocalDate plannedDate = calculatePlannedDate(customer.getRegisterdate(), fpp.getPhasedate());
                phase.setPlanneddate(plannedDate);
                phase.setFullpaiddate(null);

                // 양방향 관계
                phase.setCustomer(customer);
                phases.add(phase);
            }
            customer.setPhases(phases);
        }

        // Status가 없으면 새로 만들기
        if (customer.getStatus() == null) {
            Status status = new Status();
            status.setCustomer(customer);
            customer.setStatus(status);
        }

        // 고객 DB 저장 & 전체 재계산
        customer = customerRepository.save(customer);
        recalculateEverything(customer);

        return customer;
    }

    // ================================================
    // 2) 전체 재계산 (핵심 로직)
    // ================================================
    /**
     *  전체 재계산:
     *  (1) 각 phase의 charged/feesum/fullpaiddate 초기화
     *  (2) 모든 depositHistory를 처리하여 하나씩 분배(distribute)
     *  (3) leftover(loanExceedAmount 등) 갱신
     *  (4) Status(미납금 등) 업데이트
     *  (5) Loan 필드 업데이트 (합산)
     *  (6) 최종 저장
     */
    public void recalculateEverything(Customer customer) {
        // 1) 각 Phase 초기화
        if (customer.getPhases() != null) {
            for (Phase phase : customer.getPhases()) {
                // 기본값 초기화
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

        // 2) phase별 누적 입금액 기록용 맵 초기화
        Map<Integer, Long> cumulativeDeposits = new HashMap<>();
        if (customer.getPhases() != null) {
            for (Phase p : customer.getPhases()) {
                cumulativeDeposits.put(p.getPhaseNumber(), 0L);
            }
        }

        // 3) DepositHistory를 처리하여, leftover 계산
        List<DepositHistory> histories = customer.getDepositHistories();
        long leftoverGeneral = 0L;
        long leftoverLoan = 0L;

        // "첫 대출/자납" 판단용
        boolean anyLoanExists = false;  // 과거 대출 납부여부
        boolean anySelfExists = false;  // 과거 자납 납부여부

        if (histories != null && !histories.isEmpty()) {
            // 수정: 대출/자납 기록을 일반 입금보다 먼저 처리하도록 정렬
            histories.sort((dh1, dh2) -> {
                boolean isLoan1 = "o".equalsIgnoreCase(dh1.getLoanStatus());
                boolean isLoan2 = "o".equalsIgnoreCase(dh2.getLoanStatus());
                if (isLoan1 && !isLoan2) return -1;
                if (!isLoan1 && isLoan2) return 1;
                return dh1.getTransactionDateTime().compareTo(dh2.getTransactionDateTime());
            });

            for (DepositHistory dh : histories) {
                // (a) loanRecord, selfRecord 세팅
                if ("o".equalsIgnoreCase(dh.getLoanStatus()) && dh.getLoanDetails() != null) {
                    Long loanA = dh.getLoanDetails().getLoanammount();
                    if (loanA != null && loanA > 0) {
                        if (!anyLoanExists) {
                            dh.setLoanRecord("1");
                            anyLoanExists = true;
                        } else {
                            dh.setLoanRecord("0");
                        }
                    }
                    Long selfA = dh.getLoanDetails().getSelfammount();
                    if (selfA != null && selfA > 0) {
                        if (!anySelfExists) {
                            dh.setSelfRecord("1");
                            anySelfExists = true;
                        } else {
                            dh.setSelfRecord("0");
                        }
                    }
                }

                // (b) 각 depositHistory별 분배
                long leftover = distributeDepositPaymentToPhases(customer, dh, cumulativeDeposits);
                // (c) leftover 정리 (대출/자납 leftoverLoan, 일반 leftoverGeneral)
                if ("o".equalsIgnoreCase(dh.getLoanStatus())) {
                    leftoverLoan += leftover;
                } else {
                    leftoverGeneral += leftover;
                }

                // depositHistory에 변경사항 반영 후 DB 저장
                depositHistoryRepository.save(dh);
            }
        }

        // 4) leftover를 Status에 저장
        Status st = customer.getStatus();
        if (st == null) {
            st = new Status();
            st.setCustomer(customer);
            customer.setStatus(st);
        }
        st.setExceedamount(leftoverGeneral);
        st.setLoanExceedAmount(leftoverLoan);

        // 5) Status(미납금, unpaidphase 등) 업데이트
        updateStatusFields(customer);
        // 6) Loan 필드 업데이트 (히스토리 전체 합산)
        updateLoanField(customer);
        // 7) 최종 저장
        customerRepository.save(customer);
    }

    // ================================================
    // 3) 분배 로직(distributeDepositPaymentToPhases)
    // ================================================
    /**
     * 하나의 DepositHistory를 처리하여 Phase별로 입금액을 분배,
     * leftover를 반환.
     *
     * - [일반 입금]: (feesum - discount) 기준으로 1차부터 차례대로
     * - [대출/자납(loanStatus='o')]:
     *   1) 만약 첫 대출/자납(loanRecord='1')이면 leftover=0
     *   2) 두 번째 이후(loanRecord='0')이면 leftover=Status.loanExceedAmount를 불러옴
     *   3) totalDeposit = leftover + depositAmount
     *   4) targetPhases 순으로(할인 discount 무시)
     *   5) 남은 leftover 반환
     */
    public long distributeDepositPaymentToPhases(Customer customer,
                                                 DepositHistory dh,
                                                 Map<Integer, Long> cumulativeDeposits) {

        // (A) 대출/자납 여부
        boolean isLoanDeposit = "o".equalsIgnoreCase(dh.getLoanStatus());

        // (B) leftoverFromStatus
        long leftoverFromStatus = 0L;
        if (isLoanDeposit) {
            // 첫 대출/자납이면 leftover=0, 두 번째 이상이면 leftover=Status.loanExceedAmount
            if ("0".equals(dh.getLoanRecord())) { // 두 번째 이상
                leftoverFromStatus = (customer.getStatus() != null &&
                        customer.getStatus().getLoanExceedAmount() != null)
                        ? customer.getStatus().getLoanExceedAmount()
                        : 0L;
            }
        }

        // (C) 총 입금액
        long depositAmt = (dh.getDepositAmount() != null ? dh.getDepositAmount() : 0L);
        long totalDeposit = leftoverFromStatus + depositAmt;

        // (D) Phase 분배
        long remaining = totalDeposit;
        List<Phase> phases = customer.getPhases();
        if (phases != null) {
            phases.sort(Comparator.comparingInt(Phase::getPhaseNumber));
        }

        if (isLoanDeposit) {
            // 대출/자납 → targetPhases 만 (discount 무시)
            List<Integer> targetList = dh.getTargetPhases();
            if (targetList != null && !targetList.isEmpty()) {
                for (Integer phaseNo : targetList) {
                    Phase phase = findPhaseByNumber(phases, phaseNo);
                    if (phase == null) continue;

                    long already = cumulativeDeposits.getOrDefault(phaseNo, 0L);
                    long feesum = (phase.getFeesum() != null) ? phase.getFeesum() : 0L;

                    long required = feesum - already; // discount 무시
                    if (required <= 0) continue;

                    long allocation = Math.min(remaining, required);
                    if (allocation > 0) {
                        boolean wasZero = (already == 0L);
                        already += allocation;
                        remaining -= allocation;
                        phase.setCharged(already);

                        // fullpaiddate
                        if (already >= feesum) {
                            phase.setFullpaiddate(
                                    dh.getTransactionDateTime() != null
                                            ? dh.getTransactionDateTime().toLocalDate()
                                            : null
                            );
                        }
                        // sum
                        phase.setSum(feesum - already);

                        // depositPhaseN → "1"(처음) or "0"(추가)
                        setDepositPhaseField(dh, phaseNo, wasZero ? "1" : "0");

                        cumulativeDeposits.put(phaseNo, already);
                    }
                    if (remaining <= 0) break;
                }
            }
        } else {
            // 일반 입금
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

                    // fullpaiddate
                    if (already >= (feesum - discount)) {
                        phase.setFullpaiddate(
                                dh.getTransactionDateTime() != null
                                        ? dh.getTransactionDateTime().toLocalDate()
                                        : null
                        );
                    }
                    phase.setSum((feesum - discount) - already);

                    setDepositPhaseField(dh, phaseNo, wasZero ? "1" : "0");
                    cumulativeDeposits.put(phaseNo, already);
                }
                if (remaining <= 0) break;
            }
        }

        // (E) leftover 반환
        return remaining;
    }

    /**
     * 특정 phaseNumber에 해당하는 Phase 찾기
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
     * depositPhaseN setter
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
    // 4) Status(미납금 등) 업데이트
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
            // 총 면제금액
            long exemptionsum = phases.stream()
                    .mapToLong(p -> (p.getExemption() != null) ? p.getExemption() : 0L)
                    .sum();
            status.setExemptionsum(exemptionsum);

            // 일반 입금 기준 미납액 = Σ((feesum - discount) - charged)
            long unpaidAmmout = phases.stream().mapToLong(p -> {
                long feesum = (p.getFeesum() != null) ? p.getFeesum() : 0L;
                long discount = (p.getDiscount() != null) ? p.getDiscount() : 0L;
                long depositPaid = (p.getCharged() != null) ? p.getCharged() : 0L;
                return ((feesum - discount) - depositPaid);
            }).sum();
            status.setUnpaidammout(unpaidAmmout);

            // 미납차수
            LocalDate today = LocalDate.now();
            List<Integer> unpaidPhases = phases.stream()
                    .filter(p -> p.getPlanneddate() != null
                            && p.getPlanneddate().isBefore(today)
                            && p.getFullpaiddate() == null)
                    .map(Phase::getPhaseNumber)
                    .sorted()
                    .collect(Collectors.toList());
            String unpaidPhaseStr = unpaidPhases.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            status.setUnpaidphase(unpaidPhaseStr);

            // 총액, 40% 등
            long ammountsum = phases.stream()
                    .mapToLong(p -> (p.getFeesum() != null) ? p.getFeesum() : 0L)
                    .sum();
            status.setAmmountsum(ammountsum);
            status.setPercent40((long) (ammountsum * 0.4));
        }
    }

    // ================================================
    // 5) Loan 필드 업데이트
    // ================================================
    /**
     * depositHistories 중 loanStatus='o'를 전부 합산하여
     * Customer.loan에 최신값 반영
     */
    public void updateLoanField(Customer customer) {
        if (customer.getDepositHistories() == null) return;

        // Loan 객체가 없으면 새로
        if (customer.getLoan() == null) {
            customer.setLoan(new Loan());
        }
        Loan customerLoan = customer.getLoan();

        // (1) 대출 총액(단순히 depositAmount 합산)
        long totalLoan = customer.getDepositHistories().stream()
                .filter(dh -> "o".equalsIgnoreCase(dh.getLoanStatus()))
                .mapToLong(dh -> (dh.getDepositAmount() != null) ? dh.getDepositAmount() : 0L)
                .sum();
        customerLoan.setLoanammount(totalLoan);

        // (2) 가장 최근 대출/자납 레코드
        DepositHistory mostRecentLoan = customer.getDepositHistories().stream()
                .filter(dh -> "o".equalsIgnoreCase(dh.getLoanStatus()))
                .max(Comparator.comparing(DepositHistory::getTransactionDateTime))
                .orElse(null);
        if (mostRecentLoan != null) {
            // loanDate, loanbank, selfdate 등 업데이트
            if (mostRecentLoan.getLoanDate() != null) {
                customerLoan.setLoandate(mostRecentLoan.getLoanDate());
            }
            if (mostRecentLoan.getLoanDetails() != null) {
                // 은행
                if (mostRecentLoan.getLoanDetails().getLoanbank() != null) {
                    customerLoan.setLoanbank(mostRecentLoan.getLoanDetails().getLoanbank());
                }
                // 자납일
                if (mostRecentLoan.getLoanDetails().getSelfdate() != null) {
                    customerLoan.setSelfdate(mostRecentLoan.getLoanDetails().getSelfdate());
                }
            }
        }

        // (3) loanselfcurrent = status.loanExceedAmount
        if (customer.getStatus() != null && customer.getStatus().getLoanExceedAmount() != null) {
            customerLoan.setLoanselfcurrent(customer.getStatus().getLoanExceedAmount());
        }

        // 최종 DB 저장
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
        // customertype="c" = 정계약
        return customerRepository.countByCustomertype("c");
    }

    public long countFullyPaidOrNotOverdueCustomers() {
        // 모든 고객 중, 연체가 없는(또는 phase가 없는) 고객 수
        List<Customer> allCustomers = customerRepository.findAll();
        LocalDate today = LocalDate.now();

        return allCustomers.stream().filter(customer -> {
            List<Phase> phases = customer.getPhases();
            if (phases == null || phases.isEmpty()) return true; // Phase 없으면 연체없음
            // 연체가 하나라도 있으면 제외
            boolean hasOverdue = phases.stream().anyMatch(phase ->
                    phase.getPlanneddate() != null
                            && phase.getPlanneddate().isBefore(today)
                            && phase.getFullpaiddate() == null
            );
            return !hasOverdue;
        }).count();
    }

    // ================================================
    // 10) 연체료 정보
    // ================================================
    public List<LateFeeInfo> getLateFeeInfos(String name, String number) {
        // 검색 조건(name, number)에 맞는 고객 목록
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
                    p.getPlanneddate() != null
                            && p.getPlanneddate().isBefore(today)
                            && p.getFullpaiddate() == null
            ).collect(Collectors.toList());

            LateFeeInfo info = new LateFeeInfo();
            info.setId(c.getId());
            info.setCustomertype(c.getCustomertype() != null ? c.getCustomertype() : "N/A");
            info.setName((c.getCustomerData() != null && c.getCustomerData().getName() != null)
                    ? c.getCustomerData().getName()
                    : "N/A");
            info.setRegisterdate(c.getRegisterdate());

            if (unpaidPhases.isEmpty()) {
                // 미납없음
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
                // 연체가 있음
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

                // 예: 하루 0.05% = 0.0005
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
        // name, number 모두 있으면
        if (name != null && number != null) {
            if (number.matches("\\d+")) {
                return customerRepository.findByNameContainingAndIdContaining(name, number);
            } else {
                return customerRepository.findByCustomerDataNameContaining(name);
            }
        }
        // name만
        else if (name != null) {
            return customerRepository.findByCustomerDataNameContaining(name);
        }
        // number만
        else if (number != null) {
            if (number.matches("\\d+")) {
                return customerRepository.findByIdContaining(number);
            } else {
                return Collections.emptyList();
            }
        }
        // 둘다 없으면 전체
        else {
            return customerRepository.findAll();
        }
    }

    // ================================================
    // 12) DepositList(전체 입금 기록) DTO
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

        // 마지막 납부일
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

        // 총 입금액(=모든 차수의 charged 합)
        Long depositAmount = customer.getPhases().stream()
                .mapToLong(p -> (p.getCharged() != null) ? p.getCharged() : 0L)
                .sum();
        dto.setDepositAmount(depositAmount);

        // 은행 지점
        dto.setBankBranch(
                (customer.getFinancial() != null && customer.getFinancial().getBankname() != null)
                        ? customer.getFinancial().getBankname()
                        : ""
        );
        dto.setAccount("h");
        dto.setReservation("");

        // 1차~10차 입금 여부
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

        // 대출금액, 대출일자 (가장 최근)
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
     * 1~10차 입금 상태: charged>0 ? "o" : "x"
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
            // 알 수 없는 형식 -> 대략 100년 후로
            return registerDate.plusYears(100);
        }
    }
}
