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
    private LocalDate planneddate; //예정일자
    private LocalDate fullpaiddate; //완납일자
    private Long charge; //부담금
    private Long discount; //할인액
    private Long exemption; //면제금액
    private Long service; //업무대행비
    private String move; //이동
    private Long feesum; //n차합
    private Long charged; //낸 금액
    private Long sum; //남은금액

    @ManyToOne
    @JoinColumn(name = "customer_id")
    @JsonBackReference
    private Customer customer;
}
