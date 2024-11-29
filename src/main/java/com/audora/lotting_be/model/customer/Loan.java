// Loan.java
package com.audora.lotting_be.model.customer;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate loandate;
    private String loanbank;
    private Long loanammount;
    private LocalDate selfdate;
    private Long selfammount;
    private Long loanselfsum;
    private Long loanselfcurrent;

    @OneToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
