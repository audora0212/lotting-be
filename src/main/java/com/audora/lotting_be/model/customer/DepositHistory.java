package com.audora.lotting_be.model.customer;

import com.audora.lotting_be.model.customer.minor.Loan;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
public class DepositHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 거래일시
    private LocalDateTime transactionDateTime;

    // 적요
    private String description;

    // 기재내용 (예: 고객명 + 상태)
    private String details;

    // 계약자 (고객명 등)
    private String contractor;

    // 찾으신금액 (예: 다시 뽑은 금액)
    private Long withdrawnAmount;

    // 맡기신금액 (입금액)
    private Long depositAmount;

    // 거래후 잔액 (계산된 잔액 등, 우선 단순 기재)
    private Long balanceAfter;

    // 취급점 (은행/지점)
    private String branch;

    // 계좌 (알파벳 단축어 등)
    private String account;

    // 1~10차 입금여부 (기존 depositPhaseN 필드들)
    // "1": 이번 입금에서 해당 차수에 '처음' 돈이 들어감
    // "0": 이번 입금에서 해당 차수에 '추가' 돈이 들어감
    // null 또는 "" : 이번 입금에서 해당 차수에 분배되지 않음
    private String depositPhase1;
    private String depositPhase2;
    private String depositPhase3;
    private String depositPhase4;
    private String depositPhase5;
    private String depositPhase6;
    private String depositPhase7;
    private String depositPhase8;
    private String depositPhase9;
    private String depositPhase10;

    // 대출/자납 여부: "o" = 대출/자납 입금, 아니면 일반 입금
    private String loanStatus;

    // 대출 일자
    private LocalDate loanDate;

    // 비고 (메모)
    private String remarks;

    // ★ [임베디드] 대출/자납 상세 정보
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "loandate", column = @Column(name = "loan_details_loandate")),
            @AttributeOverride(name = "loanbank", column = @Column(name = "loan_details_loanbank")),
            @AttributeOverride(name = "loanammount", column = @Column(name = "loan_details_loanammount")),
            @AttributeOverride(name = "selfdate", column = @Column(name = "loan_details_selfdate")),
            @AttributeOverride(name = "selfammount", column = @Column(name = "loan_details_selfammount")),
            @AttributeOverride(name = "loanselfsum", column = @Column(name = "loan_details_loanselfsum")),
            @AttributeOverride(name = "loanselfcurrent", column = @Column(name = "loan_details_loanselfcurrent"))
    })
    private Loan loanDetails;

    // ★ [신규] 어느 phase에 얼마를 분배할지 지정 (대출/자납용)
    @ElementCollection
    @CollectionTable(name = "deposit_history_target_phases",
            joinColumns = @JoinColumn(name = "deposit_history_id"))
    @Column(name = "target_phase")
    private List<Integer> targetPhases;

    // ★ [신규] 첫 번째 대출이면 "1", 두 번째 이상이면 "0"
    private String loanRecord;

    // ★ [신규] 첫 번째 자납이면 "1", 두 번째 이상이면 "0"
    private String selfRecord;

    // DepositHistory는 하나의 고객에 종속됨
    @ManyToOne
    @JoinColumn(name = "customer_id")
    @JsonBackReference(value = "customer-depositHistories")
    private Customer customer;

    private String allocationDetail; //기록용
}
