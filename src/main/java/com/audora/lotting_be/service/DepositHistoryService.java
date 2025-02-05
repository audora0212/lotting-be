// src/main/java/com/audora/lotting_be/service/DepositHistoryService.java
package com.audora.lotting_be.service;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.DepositHistory;
import com.audora.lotting_be.model.customer.Phase;
import com.audora.lotting_be.repository.CustomerRepository;
import com.audora.lotting_be.repository.DepositHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DepositHistoryService {

    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerService customerService; // 고객 상태 업데이트용

    /**
     * 입금내역을 생성하고, 고객의 납입 차수(Phase)에 입금액을 낮은 번호(미납)부터 자동 분배합니다.
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

        long depositRemaining = depositHistory.getDepositAmount() != null ? depositHistory.getDepositAmount() : 0;

        // 고객의 납입 차수(Phase) 리스트를 가져오고, null이면 빈 리스트로 초기화합니다.
        List<Phase> phases = customer.getPhases();
        if (phases == null) {
            phases = new ArrayList<>();
            customer.setPhases(phases);
        }

        // phaseNumber 오름차순으로 정렬 (낮은 차수부터 입금액을 분배)
        phases.sort(Comparator.comparingInt(Phase::getPhaseNumber));

        // 각 Phase에 대해 depositRemaining을 분배
        for (Phase phase : phases) {
            if (depositRemaining <= 0) break;

            long alreadyDeposited = phase.getCharged() != null ? phase.getCharged() : 0;
            long required = phase.getFeesum() != null ? phase.getFeesum() - alreadyDeposited : 0;
            if (required <= 0) {
                // 이미 완납된 차수이면 flag "o"를 설정
                setDepositPhaseFlag(depositHistory, phase.getPhaseNumber(), "o");
                continue;
            }
            if (depositRemaining >= required) {
                // 해당 차수를 완전히 채울 수 있는 경우
                alreadyDeposited += required;
                depositRemaining -= required;
                phase.setCharged(alreadyDeposited);
                // 완납된 경우, fullpaiddate를 입금내역의 transactionDateTime의 날짜로 설정
                phase.setFullpaiddate(depositHistory.getTransactionDateTime().toLocalDate());
                setDepositPhaseFlag(depositHistory, phase.getPhaseNumber(), "o");
            } else {
                // 부분 납입: depositRemaining만큼만 추가
                alreadyDeposited += depositRemaining;
                phase.setCharged(alreadyDeposited);
                depositRemaining = 0;
                setDepositPhaseFlag(depositHistory, phase.getPhaseNumber(), "");
            }
        }
        // 고객의 상태(Status) 업데이트 (예: 미납액, 기납부액 등 재계산)
        customerService.updateStatusFields(customer);
        customerRepository.save(customer);

        return depositHistoryRepository.save(depositHistory);
    }

    /**
     * DepositHistory의 해당 차수 컬럼에 flag 값을 설정합니다.
     * phaseNumber 값에 따라 depositPhase1 ~ depositPhase10 필드에 "o" (완납) 또는 빈 문자열("")을 설정합니다.
     */
    private void setDepositPhaseFlag(DepositHistory depositHistory, int phaseNumber, String flag) {
        switch (phaseNumber) {
            case 1:
                depositHistory.setDepositPhase1(flag);
                break;
            case 2:
                depositHistory.setDepositPhase2(flag);
                break;
            case 3:
                depositHistory.setDepositPhase3(flag);
                break;
            case 4:
                depositHistory.setDepositPhase4(flag);
                break;
            case 5:
                depositHistory.setDepositPhase5(flag);
                break;
            case 6:
                depositHistory.setDepositPhase6(flag);
                break;
            case 7:
                depositHistory.setDepositPhase7(flag);
                break;
            case 8:
                depositHistory.setDepositPhase8(flag);
                break;
            case 9:
                depositHistory.setDepositPhase9(flag);
                break;
            case 10:
                depositHistory.setDepositPhase10(flag);
                break;
            default:
                break;
        }
    }
}
