// Status.java
package com.audora.lotting_be.model.customer;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(exclude = "customer") // customer 필드 제외
public class Status {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long exemptionsum; // 총면제금액
    private Long unpaidammout; // 미납금액
    private String unpaidphase; // 미납차순 (1,2,3,...)
    private Long prepaidammount; // 기납부금액
    private Long ammountsum; // 1~n차 납입총액
    private Long percent40; // 40%

    // 새로 추가된 필드들
    private Long exceedamount;      // 초과된 예약금(입금) 금액
    private Long loanExceedAmount;  // 초과된 대출/자납 금액

    @OneToOne
    @JoinColumn(name = "customer_id")
    @JsonBackReference
    private Customer customer;
}
