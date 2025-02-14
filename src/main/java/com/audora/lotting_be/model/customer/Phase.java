package com.audora.lotting_be.model.customer;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Phase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer phaseNumber;      // 차수
    private LocalDate planneddate;      // 예정일자
    private String planneddateString;   // 원래 예정일자 (문자열)
    private LocalDate fullpaiddate;     // 완납일자
    private Long charge;              // 부담금 (원금)
    private Long discount;            // 할인액 (실제 납부액에서는 차감)
    private Long exemption;           // 면제금액
    private Long service;             // 업무대행비
    private Long feesum;              // 총 부담금 = charge + service - exemption
    private String move;//이동
    private Long charged;           // 입금(Deposit)으로 지급된 금액 (할인액 반영)
    private Long loanCharged;       // 대출/자납으로 지급된 금액 (할인액 무시; 인정금액)
    private Long sum;               // 미납금 = (feesum - discount) - charged

    @ManyToOne
    @JoinColumn(name = "customer_id")
    @JsonBackReference
    private Customer customer;
}
