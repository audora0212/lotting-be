// src/main/java/com/audora/lotting_be/model/refund/CancelledCustomerRefund.java
package com.audora.lotting_be.model.refund;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class CancelledCustomerRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 고객 기본 정보
    private String name;                // 성명
    private String residentNumber;      // 주민번호
    private String source;              // 출처 (예: 가입경로 등)

    // 납입 정보
    private LocalDate paymentDate;      // 납입금일자
    private Long paymentAmount;         // 납입금액

    // 해지/환불 정보
    private LocalDate cancelDate;       // 해지일자
    private LocalDate refundDate;       // 환급일자
    private Long refundAmount;          // 환급금

    // 금융 정보
    private String institution;         // 기관 (은행명)
    private String accountNumber;       // 계좌번호

    // 기타 정보
    private String depositor;           // 입금자
    private String managerGeneral;      // 담당자총괄
    private String managerDivision;     // 담당본부
    private String managerTeam;         // 담당팀
    private String managerName;         // 담당성함
    private String reason;              // 사유
    private String remarks;             // 비고

    // 어느 고객의 해지 기록인지 식별하기 위한 고객 ID
    private Integer customerId;
}
