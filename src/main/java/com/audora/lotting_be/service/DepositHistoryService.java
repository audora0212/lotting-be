package com.audora.lotting_be.service;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.DepositHistory;
import com.audora.lotting_be.repository.CustomerRepository;
import com.audora.lotting_be.repository.DepositHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositHistoryService {

    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerService customerService;

    /**
     * [생성] 입금내역을 생성한 뒤, 전체 재계산
     */
    @Transactional
    public DepositHistory createDepositHistory(DepositHistory depositHistory) {
        if (depositHistory.getCustomer() == null || depositHistory.getCustomer().getId() == null) {
            throw new IllegalArgumentException("입금내역 생성 시 고객 ID 정보가 필요합니다.");
        }
        // 1) 고객 조회
        Customer customer = customerRepository.findById(depositHistory.getCustomer().getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 고객을 찾을 수 없습니다."));

        // 2) 저장
        depositHistory.setCustomer(customer);
        DepositHistory saved = depositHistoryRepository.save(depositHistory);

        // 3) 전체 재계산 (loanRecord / selfRecord는 recalcEverything 내부에서 결정)
        customerService.recalculateEverything(customer);

        // 4) 최종 반영된 depositHistory 다시 조회하여 반환(선택)
        saved = depositHistoryRepository.findById(saved.getId()).orElse(saved);
        return saved;
    }

    /**
     * [수정] 수정 후 전체 재계산
     */
    @Transactional
    public DepositHistory updateDepositHistory(Long id, DepositHistory updatedDepositHistory) {
        DepositHistory existing = depositHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("입금내역을 찾을 수 없습니다."));

        Customer customer = existing.getCustomer();
        if (updatedDepositHistory.getCustomer() != null && updatedDepositHistory.getCustomer().getId() != null) {
            if (!customer.getId().equals(updatedDepositHistory.getCustomer().getId())) {
                throw new IllegalArgumentException("해당 입금내역의 고객 ID가 일치하지 않습니다.");
            }
        }

        // 필드들 업데이트
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
        existing.setLoanDetails(updatedDepositHistory.getLoanDetails());
        existing.setTargetPhases(updatedDepositHistory.getTargetPhases());

        // loanRecord, selfRecord는 recalcEverything 시점에서 다시 계산하므로
        // 굳이 여기서 setLoanRecord(...)할 필요가 없거나, null로 유지 가능.
        // existing.setLoanRecord(updatedDepositHistory.getLoanRecord());
        // existing.setSelfRecord(updatedDepositHistory.getSelfRecord());

        // 저장
        DepositHistory saved = depositHistoryRepository.save(existing);

        // 전체 재계산
        customerService.recalculateEverything(customer);

        // 다시 조회하여 반환(옵션)
        saved = depositHistoryRepository.findById(saved.getId()).orElse(saved);
        return saved;
    }

    /**
     * [삭제] 삭제 후 전체 재계산
     */
    @Transactional
    public void deleteDepositHistory(Long id) {
        DepositHistory dh = depositHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("입금내역을 찾을 수 없습니다."));
        Customer customer = dh.getCustomer();

        if (customer.getDepositHistories() != null) {
            customer.getDepositHistories().remove(dh);
        }
        depositHistoryRepository.delete(dh);

        // 전체 재계산
        customerService.recalculateEverything(customer);
    }
}
