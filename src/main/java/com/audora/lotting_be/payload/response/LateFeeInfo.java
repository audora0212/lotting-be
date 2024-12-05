// src/main/java/com/audora/lotting_be/payload/response/LateFeeInfo.java
package com.audora.lotting_be.payload.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LateFeeInfo {
    private Integer id; // 관리번호
    private Integer lastUnpaidPhaseNumber; // 마지막 미납 차수
    private String customertype; // 고객 유형
    private String name; // 성명
    private LocalDate registerdate; // 가입일자
    private LocalDate lateBaseDate; // 연체기준일
    private LocalDate recentPaymentDate; // 최근납부일자
    private Long daysOverdue; // 일수
    private Double lateRate; // 연체율 (%)
    private Long overdueAmount; // 연체금액
    private Long paidAmount; // 납입금액
    private Double lateFee; // 연체료
    private Long totalOwed; // 내야할 돈 합계
}
