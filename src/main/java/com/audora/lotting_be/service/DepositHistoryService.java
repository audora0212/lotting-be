// src/main/java/com/audora/lotting_be/service/DepositHistoryService.java
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
     * [생성] 입금내역을 생성한 뒤, 전체 재계산 및 loan 필드 업데이트
     */
    @Transactional
    public DepositHistory createDepositHistory(DepositHistory depositHistory) {
        if (depositHistory.getCustomer() == null || depositHistory.getCustomer().getId() == null) {
            throw new IllegalArgumentException("입금내역 생성 시 고객 ID 정보가 필요합니다.");
        }
        // 고객 조회
        Customer customer = customerRepository.findById(depositHistory.getCustomer().getId())
                .orElseThrow(() -> new IllegalArgumentException("고객을 찾을 수 없습니다."));

        depositHistory.setCustomer(customer);
        DepositHistory saved = depositHistoryRepository.save(depositHistory);

        // 전체 재계산 및 loan 필드 업데이트 (내부에서 updateLoanField 호출)
        customerService.recalculateEverything(customer);

        return saved;
    }

    /**
     * [수정] 수정 후 전체 재계산 및 loan 필드 업데이트
     */
    @Transactional
    public DepositHistory updateDepositHistory(Long id, DepositHistory updatedDepositHistory) {
        DepositHistory existing = depositHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("입금내역을 찾을 수 없습니다."));

        // 고객 확인
        Customer customer = existing.getCustomer();
        if (updatedDepositHistory.getCustomer() != null && updatedDepositHistory.getCustomer().getId() != null) {
            if (!customer.getId().equals(updatedDepositHistory.getCustomer().getId())) {
                throw new IllegalArgumentException("해당 입금내역의 고객 ID가 일치하지 않습니다.");
            }
        }

        // 필드 업데이트
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

        DepositHistory saved = depositHistoryRepository.save(existing);

        // 전체 재계산 및 loan 필드 업데이트
        customerService.recalculateEverything(customer);

        return saved;
    }

    /**
     * [삭제] 삭제 후 전체 재계산 및 loan 필드 업데이트
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

        // 전체 재계산 및 loan 필드 업데이트
        customerService.recalculateEverything(customer);
    }
}
