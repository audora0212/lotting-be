// DepositHistoryService.java
package com.audora.lotting_be.service;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.DepositHistory;
import com.audora.lotting_be.repository.CustomerRepository;
import com.audora.lotting_be.repository.DepositHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DepositHistoryService {

    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerService customerService; // 고객 상태 업데이트용

    /**
     * 입금내역을 생성하고, 고객의 납입 차수(Phase)에 입금액을 낮은 번호부터 자동 분배합니다.
     * 클라이언트에서는 "customer": { "id": <고객ID> } 만 전달할 수 있으므로,
     * 반드시 고객 ID를 이용해 DB에서 완전한 고객 엔티티를 재조회하여 사용합니다.
     */
    @Transactional
    public DepositHistory createDepositHistory(DepositHistory depositHistory) {
        // 고객 정보 검증: 고객 객체와 고객 ID가 반드시 존재해야 함
        if (depositHistory.getCustomer() == null || depositHistory.getCustomer().getId() == null) {
            throw new IllegalArgumentException("입금내역 생성 시 고객 ID 정보가 필요합니다.");
        }
        // DB에서 완전한 고객 엔티티를 조회 (연관 데이터 포함)
        Customer customer = customerRepository.findById(depositHistory.getCustomer().getId())
                .orElseThrow(() -> new IllegalArgumentException("고객을 찾을 수 없습니다."));
        // 조회된 고객 객체로 depositHistory의 customer를 교체
        depositHistory.setCustomer(customer);

        long depositAmount = depositHistory.getDepositAmount() != null ? depositHistory.getDepositAmount() : 0;
        LocalDateTime transactionTime = depositHistory.getTransactionDateTime() != null ?
                depositHistory.getTransactionDateTime() : LocalDateTime.now();
        // 고객의 Phase에 depositAmount 분배 (분배 후 남은 금액 반환)
        long leftover = customerService.distributePaymentToPhases(customer, depositAmount, transactionTime.toLocalDate());
        // 기존 Status.exceedamount와 누적 (있으면 더하고, 없으면 남은값 저장)
        Long currentExceed = customer.getStatus().getExceedamount() != null ? customer.getStatus().getExceedamount() : 0L;
        customer.getStatus().setExceedamount(currentExceed + leftover);

        // 고객의 상태(Status) 업데이트
        customerService.updateStatusFields(customer);
        customerRepository.save(customer);

        return depositHistoryRepository.save(depositHistory);
    }
}
