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
     * 입금내역 생성 후 전체 재계산
     * 단, 만약 depositPhase1에 예상치 못한 값이 있으면 재계산을 유도하지 않습니다.
     */
    @Transactional
    public DepositHistory createDepositHistory(DepositHistory depositHistory) {
        if (depositHistory.getCustomer() == null || depositHistory.getCustomer().getId() == null) {
            throw new IllegalArgumentException("입금내역 생성 시 고객 ID 정보가 필요합니다.");
        }
        Customer customer = customerRepository.findById(depositHistory.getCustomer().getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 고객을 찾을 수 없습니다."));
        depositHistory.setCustomer(customer);
        DepositHistory saved = depositHistoryRepository.save(depositHistory);
        // 재계산 유도 전 depositPhase1이 기록용(예상치 못한 값)인지 확인
        if (depositHistory.getDepositPhase1() == null ||
                ("0".equals(depositHistory.getDepositPhase1()) || "1".equals(depositHistory.getDepositPhase1()) || "2".equals(depositHistory.getDepositPhase1()))) {
            customerService.recalculateEverything(customer);
        }
        saved = depositHistoryRepository.findById(saved.getId()).orElse(saved);
        return saved;
    }

    /**
     * 입금내역 수정 후 전체 재계산
     * 단, 해당 DepositHistory의 depositPhase1이 예상치 못한 값(기록용)이라면 재계산을 유도하지 않습니다.
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
        // 필드 업데이트 (depositPhase 필드는 그대로 유지)
        existing.setTransactionDateTime(updatedDepositHistory.getTransactionDateTime());
        existing.setDescription(updatedDepositHistory.getDescription());
        existing.setDetails(updatedDepositHistory.getDetails());
        existing.setContractor(updatedDepositHistory.getContractor());
        existing.setWithdrawnAmount(updatedDepositHistory.getWithdrawnAmount());
        existing.setDepositAmount(updatedDepositHistory.getDepositAmount());
        existing.setBranch(updatedDepositHistory.getBranch());
        existing.setAccount(updatedDepositHistory.getAccount());
        // depositPhase1~10는 변경하지 않음(기록용 값 유지)
        existing.setLoanStatus(updatedDepositHistory.getLoanStatus());
        existing.setLoanDate(updatedDepositHistory.getLoanDate());
        existing.setRemarks(updatedDepositHistory.getRemarks());
        existing.setLoanDetails(updatedDepositHistory.getLoanDetails());
        existing.setTargetPhases(updatedDepositHistory.getTargetPhases());
        DepositHistory saved = depositHistoryRepository.save(existing);
        // 재계산은 depositPhase1이 허용된 값일 때만 유도
        if (existing.getDepositPhase1() == null ||
                ("0".equals(existing.getDepositPhase1()) || "1".equals(existing.getDepositPhase1()) || "2".equals(existing.getDepositPhase1()))) {
            customerService.recalculateEverything(customer);
        }
        saved = depositHistoryRepository.findById(saved.getId()).orElse(saved);
        return saved;
    }

    /**
     * 입금내역 삭제 후 전체 재계산
     * 단, 삭제 대상 DepositHistory가 기록용으로 처리된 경우(예: depositPhase1에 예상치 못한 값이 있으면)
     * 재계산을 유도하지 않습니다.
     */
    @Transactional
    public void deleteDepositHistory(Long id) {
        DepositHistory dh = depositHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("입금내역을 찾을 수 없습니다."));
        Customer customer = dh.getCustomer();
        // 고객의 depositHistories 컬렉션에서 해당 입금내역을 제거
        if (customer.getDepositHistories() != null) {
            customer.getDepositHistories().remove(dh);
        }
        depositHistoryRepository.delete(dh);
        // 재계산: depositPhase1이 허용된 값("0","1","2")일 때만 재계산
        if (dh.getDepositPhase1() == null ||
                ("0".equals(dh.getDepositPhase1()) || "1".equals(dh.getDepositPhase1()) || "2".equals(dh.getDepositPhase1()))) {
            customerService.recalculateEverything(customer);
        }
    }

}
