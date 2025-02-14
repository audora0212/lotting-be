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

    // ======================== 1) 고객 생성 및 초기 Phase 설정 ========================
    /**
     * 고객을 새로 생성할 때,
     * - Fee 테이블에서 (군+타입+가입차순)에 해당하는 Fee를 찾아 Phase를 초기화
     * - Status도 생성(없다면)
     * - DB 저장 후, 전체 재계산을 진행한다.
     *
     * 예약금은 더 이상 자동으로 Phase에 분배하지 않고, DepositHistory를 통해 처리한다.
     */
    public Customer createCustomer(Customer customer) {
        if (customerRepository.existsById(customer.getId())) {
            throw new IllegalArgumentException("이미 존재하는 관리번호입니다.");
        }

        // Fee 조회
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
                // feesum는 원래 부담금 (charge + service - exemption)
                long feesum = charge + 0L - 0L; // 초기에는 service, exemption이 0
                phase.setFeesum(feesum);
                phase.setCharged(0L);      // 입금(Deposit) 지급액 초기화
                phase.setLoanCharged(0L);  // 대출/자납 지급액 초기화 (신규 필드)
                long discountVal = (phase.getDiscount() != null) ? phase.getDiscount() : 0L;
                // net due(실제 납부해야 할 금액)는 feesum에서 할인액를 뺀 값
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

        // 먼저 DB에 저장 (Cascade로 Phase, Status도 함께)
        customer = customerRepository.save(customer);

        // 전체 재계산 진행 (예약금은 DepositHistory로 처리)
        recalculateEverything(customer);

        return customer;
    }

    // ======================== 2) "전체 재계산" 로직  ========================
    /**
     * (a) 모든 Phase의 deposit 및 loan 관련 금액 초기화
     * (b) DepositHistory의 입금내역을 통해 deposit payment 분배
     * (c) 대출/자납액(Loan) 분배: 할인액을 무시하고 전체 부담금을 기준으로 분배
     * (d) leftover(초과분)을 status에 기록
     * (e) updateStatusFields를 호출하여 미납 금액 등을 갱신
     * (f) 최종 DB 저장
     */
    public void recalculateEverything(Customer customer) {
        // 1) Phase 초기화: 입금(payment)과 대출(loan) 관련 금액 초기화
        if (customer.getPhases() != null) {
            for (Phase phase : customer.getPhases()) {
                phase.setCharged(0L);      // 입금 지급액 초기화
                phase.setLoanCharged(0L);  // 대출 지급액 초기화
                phase.setFullpaiddate(null);
                long charge = (phase.getCharge() != null) ? phase.getCharge() : 0L;
                long service = (phase.getService() != null) ? phase.getService() : 0L;
                long exemption = (phase.getExemption() != null) ? phase.getExemption() : 0L;
                long feesum = charge + service - exemption;
                phase.setFeesum(feesum);
                long discountVal = (phase.getDiscount() != null) ? phase.getDiscount() : 0L;
                // 입금 시에는 net due = feesum - discount
                phase.setSum(feesum - discountVal);
            }
        }

        // 2) 기존 예약금(Deposit) 분배 제거 – 이제 예약금은 DepositHistory로 처리함
        // (이 부분의 코드를 제거합니다.)

        // 3) 입금내역(DepositHistory) 분배 – deposit payment(할인액 반영)
        long depositHistLeftoverTotal = 0L;
        List<DepositHistory> histories = customer.getDepositHistories();
        if (histories != null && !histories.isEmpty()) {
            histories.sort(Comparator.comparing(DepositHistory::getTransactionDateTime));
            for (DepositHistory dh : histories) {
                long amount = (dh.getDepositAmount() != null) ? dh.getDepositAmount() : 0L;
                LocalDate txDate = (dh.getTransactionDateTime() != null)
                        ? dh.getTransactionDateTime().toLocalDate()
                        : customer.getRegisterdate();
                long leftover = distributeDepositPaymentToPhases(customer, amount, txDate);
                depositHistLeftoverTotal += leftover;
            }
        }

        // 4) 대출/자납 분배 – loan payment(할인액 무시, 전체 부담금을 기준)
        long loanExceed = 0L; // 대출 남은액
        if (customer.getLoan() != null) {
            long loanAmount = (customer.getLoan().getLoanammount() != null)
                    ? customer.getLoan().getLoanammount() : 0L;
            long selfAmount = (customer.getLoan().getSelfammount() != null)
                    ? customer.getLoan().getSelfammount() : 0L;
            long totalLoan = loanAmount + selfAmount;

            if (totalLoan > 0) {
                LocalDate loanDate = null;
                if (customer.getLoan().getLoandate() != null) {
                    loanDate = customer.getLoan().getLoandate();
                } else if (customer.getLoan().getSelfdate() != null) {
                    loanDate = customer.getLoan().getSelfdate();
                } else {
                    loanDate = customer.getRegisterdate();
                }
                loanExceed = distributeLoanPaymentToPhases(customer, totalLoan, loanDate);

                // Loan 엔티티 내부 계산: 인식된 대출 금액 = totalLoan - leftover
                customer.getLoan().setLoanselfsum(totalLoan - loanExceed);
                customer.getLoan().setLoanselfcurrent(loanExceed);
            } else {
                customer.getLoan().setLoanselfsum(0L);
                customer.getLoan().setLoanselfcurrent(0L);
            }
        }

        // 5) leftover를 status에 기록 (여기서는 입금내역의 leftover만 기록)
        long depositAndHistoryLeftover = depositHistLeftoverTotal;
        Status st = customer.getStatus();
        if (st == null) {
            st = new Status();
            st.setCustomer(customer);
            customer.setStatus(st);
        }
        st.setExceedamount(depositAndHistoryLeftover);
        st.setLoanExceedAmount(loanExceed);

        // 6) 상태 필드 업데이트 (미납금 등 deposit payment 기준)
        updateStatusFields(customer);

        // 7) 최종 DB 저장
        customerRepository.save(customer);
    }

    /**
     * DepositHistory 등 일반 입금(예약금 제외) 분배 – 할인액을 반영하여 net due 계산
     */
    public long distributeDepositPaymentToPhases(Customer customer, long paymentAmount, LocalDate paymentDate) {
        List<Phase> phases = customer.getPhases();
        if (phases == null || phases.isEmpty()) {
            return paymentAmount;
        }
        // 차수 순 정렬
        phases.sort(Comparator.comparingInt(Phase::getPhaseNumber));
        long remaining = paymentAmount;
        for (Phase phase : phases) {
            long already = (phase.getCharged() != null) ? phase.getCharged() : 0L;
            long feesum = (phase.getFeesum() != null) ? phase.getFeesum() : 0L;
            long discount = (phase.getDiscount() != null) ? phase.getDiscount() : 0L;
            long netDue = feesum - discount;
            long required = netDue - already;
            if (required <= 0) {
                continue;
            }
            if (remaining >= required) {
                phase.setCharged(already + required);
                phase.setSum(netDue - (already + required));
                if (phase.getFullpaiddate() == null) {
                    phase.setFullpaiddate(paymentDate);
                }
                remaining -= required;
            } else {
                phase.setCharged(already + remaining);
                phase.setSum(netDue - (already + remaining));
                remaining = 0;
                break;
            }
        }
        return remaining;
    }

    /**
     * 대출/자납액 분배 – 할인액 무시하고 전체 부담금을 기준으로 분배
     */
    public long distributeLoanPaymentToPhases(Customer customer, long paymentAmount, LocalDate paymentDate) {
        List<Phase> phases = customer.getPhases();
        if (phases == null || phases.isEmpty()) {
            return paymentAmount;
        }
        // 차수 순 정렬
        phases.sort(Comparator.comparingInt(Phase::getPhaseNumber));
        long remaining = paymentAmount;
        for (Phase phase : phases) {
            long already = (phase.getLoanCharged() != null) ? phase.getLoanCharged() : 0L;
            long feesum = (phase.getFeesum() != null) ? phase.getFeesum() : 0L;
            long required = feesum - already;  // 할인액 무시
            if (required <= 0) {
                continue;
            }
            if (remaining >= required) {
                phase.setLoanCharged(already + required);
                remaining -= required;
            } else {
                phase.setLoanCharged(already + remaining);
                remaining = 0;
                break;
            }
        }
        return remaining;
    }

    /**
     * Phase에 따라 미납금, 미납 차수 등을 Status 필드에 갱신 (입금 payment 기준 – 할인액 반영)
     */
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

            long unpaidAmmout = phases.stream().mapToLong(p -> {
                long feesum = (p.getFeesum() != null) ? p.getFeesum() : 0L;
                long discount = (p.getDiscount() != null) ? p.getDiscount() : 0L;
                long depositPaid = (p.getCharged() != null) ? p.getCharged() : 0L;
                long netDue = feesum - discount - depositPaid;
                return netDue;
            }).sum();
            status.setUnpaidammout(unpaidAmmout);

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

            long ammountsum = phases.stream()
                    .mapToLong(p -> (p.getFeesum() != null) ? p.getFeesum() : 0L)
                    .sum();
            status.setAmmountsum(ammountsum);

            status.setPercent40((long) (ammountsum * 0.4));
            // exceedamount 및 loanExceedAmount는 recalculateEverything에서 셋팅됨.
        }
    }

    // ======================== 3) 그 외 메서드 (검색, 조회, 삭제 등) ========================

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
        } else {
            return false;
        }
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
                            phase.getFullpaiddate() == null
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

        Long depositAmount = customer.getPhases().stream()
                .mapToLong(p -> p.getCharged() != null ? p.getCharged() : 0L)
                .sum();
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

    // ----------------------- 내부 헬퍼: 예정일자 계산 -------------------------
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

    public void deleteCustomer(Integer id) {
        customerRepository.deleteById(id);
    }
}
