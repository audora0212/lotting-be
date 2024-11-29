// Status.java
package com.audora.lotting_be.model.customer;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Status {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long exemptionsum;
    private Long unpaidammout;
    private String unpaidphase;
    private Long prepaidammount;
    private Long ammountsum;
    private Long percent40;

    @OneToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
