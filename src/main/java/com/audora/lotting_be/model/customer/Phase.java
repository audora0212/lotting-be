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

    private Integer phaseNumber;
    private LocalDate planneddate;
    private LocalDate fullpaiddate;
    private Long charge;
    private Long discount;
    private Long exemption;
    private Long service;
    private String move;
    private Long feesum;
    private Long charged;
    private Long sum;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    @JsonBackReference
    private Customer customer;
}
