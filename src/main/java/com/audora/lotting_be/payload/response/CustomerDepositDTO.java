package com.audora.lotting_be.payload.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CustomerDepositDTO {

    // 회원번호
    private Integer memberNumber;

    // 마지막 거래 일시
    private LocalDateTime lastTransactionDateTime;

    // 적요 (임시)
    private String remarks;

    // 기재내용 (메모 역할)
    private String memo;

    // 계약자
    private String contractor;

    // 찾으신 금액 (환불받은 금액)
    private Long withdrawnAmount;

    // 맡기신 금액 (지금까지 입금한 금액)
    private Long depositAmount;

    // 취급점(은행/지점)
    private String bankBranch;

    // 계좌 (h, g, f, e 중 하나)
    private String account;

    // 예약 (의미 불명확, 임시)
    private String reservation;

    // 1차~10차 입금 상태
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

    // 대출금액
    private Long loanAmount;

    // 대출일자
    private LocalDate loanDate;

    // 임시
    private String temporary;

    // 비고
    private String note;
}
