// src/main/java/com/audora/lotting_be/model/customer/DepositHistory.java
package com.audora.lotting_be.model.customer;

import com.audora.lotting_be.model.customer.minor.Loan;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    // 각 납입차수별 입금 완료 여부 (입금 완료면 "o", 아니면 공백)
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

    // 대출 여부 (대출 시 "o"로 표시)
    private String loanStatus;
    // 대출 일자 (DepositHistory의 loanDate – naming 전략에 따라 "loan_date"로 매핑됨)
    private LocalDate loanDate;
    // 비고 (메모)
    private String remarks;

    // ★ 신규 추가: 대출/자납 입금 시 관련 Loan 정보 기록 (기존 Loan과 동일한 구조)
    // 단, 컬럼명이 충돌하지 않도록 @AttributeOverrides로 별도 이름 지정
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

    // DepositHistory는 하나의 고객에 종속됨
    @ManyToOne
    @JoinColumn(name = "customer_id")
    @JsonBackReference(value = "customer-depositHistories")
    private Customer customer;
}
