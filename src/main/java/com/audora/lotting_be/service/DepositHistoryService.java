package com.audora.lotting_be.service;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.DepositHistory;
import com.audora.lotting_be.repository.CustomerRepository;
import com.audora.lotting_be.repository.DepositHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class DepositHistoryService {

    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerService customerService; // 고객 분배 관련 업데이트용

    /**
     * [생성] 입금내역을 생성하고, 해당 입금액을 고객의 Phase에 분배하여 상태를 업데이트합니다.
     */
    @Transactional
    public DepositHistory createDepositHistory(DepositHistory depositHistory) {
        if (depositHistory.getCustomer() == null || depositHistory.getCustomer().getId() == null) {
            throw new IllegalArgumentException("입금내역 생성 시 고객 ID 정보가 필요합니다.");
        }
        // 고객 엔티티 재조회(연관 데이터 포함)
        Customer customer = customerRepository.findById(depositHistory.getCustomer().getId())
                .orElseThrow(() -> new IllegalArgumentException("고객을 찾을 수 없습니다."));
        depositHistory.setCustomer(customer);

        Long depositAmount = depositHistory.getDepositAmount() != null ? depositHistory.getDepositAmount() : 0;
        LocalDateTime transactionTime = depositHistory.getTransactionDateTime() != null ?
                depositHistory.getTransactionDateTime() : LocalDateTime.now();

        // 고객의 Phase에 depositAmount 분배 (분배 후 남은 금액 반환)
        long leftover = customerService.distributePaymentToPhases(customer, depositAmount, transactionTime.toLocalDate());
        Long currentExceed = customer.getStatus().getExceedamount() != null ? customer.getStatus().getExceedamount() : 0L;
        customer.getStatus().setExceedamount(currentExceed + leftover);

        // 고객의 상태 업데이트 후 저장
        customerService.updateStatusFields(customer);
        customerRepository.save(customer);

        return depositHistoryRepository.save(depositHistory);
    }

    /**
     * [수정] 기존 입금내역을 수정한 후, 해당 고객의 모든 입금내역을 기반으로 분배 내역을 재계산합니다.
     */
    @Transactional
    public DepositHistory updateDepositHistory(Long id, DepositHistory updatedDepositHistory) {
        DepositHistory existing = depositHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("입금내역을 찾을 수 없습니다."));

        Customer customer = existing.getCustomer();
        // 만약 수정 요청에 고객 ID가 포함되어 있다면 일치 여부 확인
        if (updatedDepositHistory.getCustomer() != null && updatedDepositHistory.getCustomer().getId() != null) {
            if (!customer.getId().equals(updatedDepositHistory.getCustomer().getId())) {
                throw new IllegalArgumentException("고객 ID가 일치하지 않습니다.");
            }
        }

        // 수정할 필드 모두 업데이트
        existing.setTransactionDateTime(updatedDepositHistory.getTransactionDateTime());
        existing.setDescription(updatedDepositHistory.getDescription());
        existing.setDetails(updatedDepositHistory.getDetails());
        existing.setContractor(updatedDepositHistory.getContractor());
        existing.setWithdrawnAmount(updatedDepositHistory.getWithdrawnAmount());
        existing.setDepositAmount(updatedDepositHistory.getDepositAmount());
        existing.setBranch(updatedDepositHistory.getBranch());
        existing.setAccount(updatedDepositHistory.getAccount());
        existing.setDepositPhase1(updatedDepositHistory.getDepositPhase1());
        existing.setDepositPhase2(updatedDepositHistory.getDepositPhase2());
        existing.setDepositPhase3(updatedDepositHistory.getDepositPhase3());
        existing.setDepositPhase4(updatedDepositHistory.getDepositPhase4());
        existing.setDepositPhase5(updatedDepositHistory.getDepositPhase5());
        existing.setDepositPhase6(updatedDepositHistory.getDepositPhase6());
        existing.setDepositPhase7(updatedDepositHistory.getDepositPhase7());
        existing.setDepositPhase8(updatedDepositHistory.getDepositPhase8());
        existing.setDepositPhase9(updatedDepositHistory.getDepositPhase9());
        existing.setDepositPhase10(updatedDepositHistory.getDepositPhase10());
        existing.setLoanStatus(updatedDepositHistory.getLoanStatus());
        existing.setLoanDate(updatedDepositHistory.getLoanDate());
        existing.setRemarks(updatedDepositHistory.getRemarks());

        // 수정 후 고객에 속한 모든 입금내역을 반영하여 분배 내역을 재계산
        recalculateDepositHistoryDistribution(customer);

        return depositHistoryRepository.save(existing);
    }

    /**
     * [삭제] 입금내역을 삭제한 후, 해당 고객의 분배 내역을 재계산합니다.
     */
    @Transactional
    public void deleteDepositHistory(Long id) {
        DepositHistory depositHistory = depositHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("입금내역을 찾을 수 없습니다."));
        Customer customer = depositHistory.getCustomer();
        // 고객의 depositHistories 리스트에서 삭제(양방향 연관관계 관리)
        if (customer.getDepositHistories() != null) {
            customer.getDepositHistories().removeIf(dh -> dh.getId().equals(id));
        }
        depositHistoryRepository.delete(depositHistory);
        recalculateDepositHistoryDistribution(customer);
    }

    /**
     * 해당 고객의 모든 입금내역을 시간순(오름차순)으로 적용하여 Phase 분배를 재계산합니다.
     * – 우선 CustomerService의 recalculatePaymentDistribution()을 호출하여 예약금, 대출/자납 분배를 초기화한 후,
     * – 고객에 속한 모든 DepositHistory를 순차적으로 적용합니다.
     */
    private void recalculateDepositHistoryDistribution(Customer customer) {
        // 고객의 기존 분배 내역 초기화(예약금, 대출/자납 등)
        customerService.recalculatePaymentDistribution(customer);

        long totalExceed = 0;
        List<DepositHistory> histories = customer.getDepositHistories();
        if (histories != null && !histories.isEmpty()) {
            histories.sort(Comparator.comparing(DepositHistory::getTransactionDateTime));
            for (DepositHistory dh : histories) {
                Long amount = dh.getDepositAmount() != null ? dh.getDepositAmount() : 0;
                LocalDateTime txTime = dh.getTransactionDateTime() != null ? dh.getTransactionDateTime() : LocalDateTime.now();
                long leftover = customerService.distributePaymentToPhases(customer, amount, txTime.toLocalDate());
                totalExceed += leftover;
            }
        }
        customer.getStatus().setExceedamount(totalExceed);
        customerService.updateStatusFields(customer);
        customerRepository.save(customer);
    }
}
