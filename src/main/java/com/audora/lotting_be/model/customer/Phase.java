// Phase.java
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

    private Integer phaseNumber; //차수
    private LocalDate planneddate; //예정일자 (실제 날짜 형식)
    private String planneddateString; //원래 예정일자 (~~전 ~~후와 같이 대략적으로 표기한것 저장)
    private LocalDate fullpaiddate; //완납일자 (실제 날짜 형식)
    private Long charge; //부담금 (표에서 받아온 값)
    private Long discount; //할인액 (이미 낸 금액과 동일하게 처리)
    private Long exemption; //면제금액
    private Long service; //업무대행비 (표에서 받아온 값)
    private String move; //이동 (etc)
    private Long feesum; //n차합 (총 내야하는 금액=charge+service-exemption)
    private Long charged; //낸 금액 (지불한 금액+discount)
    private Long sum; //남은금액 (feesum-charged)

    @ManyToOne
    @JoinColumn(name = "customer_id")
    @JsonBackReference
    private Customer customer;
}
