package com.audora.lotting_be.service;

import com.audora.lotting_be.model.customer.Customer;
import com.audora.lotting_be.model.customer.DepositHistory;
import com.audora.lotting_be.repository.CustomerRepository;
import com.audora.lotting_be.repository.DepositHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepositHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(DepositHistoryService.class);

    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerService customerService;


    public List<DepositHistory> getAllDepositHistories() {
        return depositHistoryRepository.findAll();
    }

    @Transactional
    public DepositHistory createDepositHistory(DepositHistory depositHistory) {
        logger.info("createDepositHistory 시작. 고객 ID: {}",
                depositHistory.getCustomer() != null ? depositHistory.getCustomer().getId() : "null");

        if (depositHistory.getCustomer() == null || depositHistory.getCustomer().getId() == null) {
            throw new IllegalArgumentException("입금내역 생성 시 고객 ID 정보가 필요합니다.");
        }
        Customer customer = customerRepository.findById(depositHistory.getCustomer().getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 고객을 찾을 수 없습니다."));
        depositHistory.setCustomer(customer);

        DepositHistory saved = depositHistoryRepository.save(depositHistory);

        try {
                        if(!customer.getId().equals(1)){
                if (depositHistory.getDepositPhase1() == null || depositHistory.getDepositPhase1().equals("") ||
                        ("0".equals(depositHistory.getDepositPhase1()) ||
                                "1".equals(depositHistory.getDepositPhase1()) ||
                                "2".equals(depositHistory.getDepositPhase1()))) {
                    logger.info("createDepositHistory: 재계산 시작 for 고객 id: {}", customer.getId());
                    customerService.recalculateEverything(customer);
                }
            }
        } catch (Exception e) {
            logger.error("createDepositHistory 중 재계산 실패, 고객 id {}: {}", customer.getId(), e.getMessage());
        }
        saved = depositHistoryRepository.findById(saved.getId()).orElse(saved);
        logger.info("createDepositHistory 완료, 저장된 DepositHistory id: {}", saved.getId());
        return saved;
    }

    @Transactional
    public DepositHistory updateDepositHistory(Long id, DepositHistory updatedDepositHistory) {
        logger.info("updateDepositHistory 시작. 대상 DepositHistory id: {}", id);
        DepositHistory existing = depositHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("입금내역을 찾을 수 없습니다."));
        Customer customer = existing.getCustomer();

        if (updatedDepositHistory.getCustomer() != null && updatedDepositHistory.getCustomer().getId() != null) {
            if (!customer.getId().equals(updatedDepositHistory.getCustomer().getId())) {
                throw new IllegalArgumentException("해당 입금내역의 고객 ID가 일치하지 않습니다.");
            }
        }

        existing.setTransactionDateTime(updatedDepositHistory.getTransactionDateTime());
        existing.setDescription(updatedDepositHistory.getDescription());
        existing.setDetails(updatedDepositHistory.getDetails());
        existing.setContractor(updatedDepositHistory.getContractor());
        existing.setWithdrawnAmount(updatedDepositHistory.getWithdrawnAmount());
        existing.setDepositAmount(updatedDepositHistory.getDepositAmount());
        existing.setBranch(updatedDepositHistory.getBranch());
        existing.setAccount(updatedDepositHistory.getAccount());
        existing.setLoanStatus(updatedDepositHistory.getLoanStatus());
        existing.setLoanDate(updatedDepositHistory.getLoanDate());
        existing.setRemarks(updatedDepositHistory.getRemarks());
        existing.setLoanDetails(updatedDepositHistory.getLoanDetails());
        existing.setTargetPhases(updatedDepositHistory.getTargetPhases());

        DepositHistory saved = depositHistoryRepository.save(existing);

        try {
            if (existing.getDepositPhase1() == null ||
                    ("0".equals(existing.getDepositPhase1()) ||
                            "1".equals(existing.getDepositPhase1()) ||
                            "2".equals(existing.getDepositPhase1()))) {
                logger.info("updateDepositHistory: 재계산 시작 for 고객 id: {}", customer.getId());
                customerService.recalculateEverything(customer);
            }
        } catch (Exception e) {
            logger.error("updateDepositHistory 중 재계산 실패, 고객 id {}: {}", customer.getId(), e.getMessage());
        }
        saved = depositHistoryRepository.findById(saved.getId()).orElse(saved);
        logger.info("updateDepositHistory 완료, 업데이트된 DepositHistory id: {}", saved.getId());
        return saved;
    }

    @Transactional
    public void deleteDepositHistory(Long id) {
        logger.info("deleteDepositHistory 시작. 대상 DepositHistory id: {}", id);
        DepositHistory dh = depositHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("입금내역을 찾을 수 없습니다."));
        Customer customer = dh.getCustomer();

        if (customer.getDepositHistories() != null) {
            customer.getDepositHistories().remove(dh);
        }
        depositHistoryRepository.delete(dh);

        try {
            customer.setDepositHistories(
                    depositHistoryRepository.findAll().stream()
                            .filter(history -> history.getCustomer().getId().equals(customer.getId()))
                            .collect(Collectors.toList())
            );
            if (dh.getDepositPhase1() == null ||
                    ("0".equals(dh.getDepositPhase1()) ||
                            "1".equals(dh.getDepositPhase1()) ||
                            "2".equals(dh.getDepositPhase1()))) {
                logger.info("deleteDepositHistory: 재계산 시작 for 고객 id: {}", customer.getId());
                customerService.recalculateEverything(customer);
            }
        } catch (Exception e) {
            logger.error("deleteDepositHistory 중 재계산 실패, 고객 id {}: {}", customer.getId(), e.getMessage());
        }
        logger.info("deleteDepositHistory 완료. 대상 DepositHistory id: {}", id);
    }

}
