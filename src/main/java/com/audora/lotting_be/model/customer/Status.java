// Status.java
package com.audora.lotting_be.model.customer;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Status {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long exemptionsum; //총면제금액
    private Long unpaidammout; //미납금액
    private String unpaidphase; //미납차순 (1,2,3,...)
    private Long prepaidammount; //기납부금액
    private Long ammountsum; //1~n차 납입총액
    private Long percent40; //40%

    @OneToOne
    @JoinColumn(name = "customer_id")
    @JsonBackReference
    private Customer customer;
}
