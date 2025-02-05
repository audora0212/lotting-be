// src/main/java/com/audora/lotting_be/model/customer/DepositHistory.java
package com.audora.lotting_be.model.customer;

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

    // 대출 여부 (대출시 "o")
    private String loanStatus;
    // 대출 일자
    private LocalDate loanDate;
    // 비고 (메모)
    private String remarks;

    // DepositHistory는 하나의 고객에 종속됨
    // 순환참조 방지를 위해 value를 "customer-depositHistories"로 지정합니다.
    @ManyToOne
    @JoinColumn(name = "customer_id")
    @JsonBackReference(value = "customer-depositHistories")
    private Customer customer;
}
