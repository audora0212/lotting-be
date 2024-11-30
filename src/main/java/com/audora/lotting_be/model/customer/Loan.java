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

    private LocalDate loandate; //대출일자
    private String loanbank; //은행
    private Long loanammount; //대출액
    private LocalDate selfdate; //자납일
    private Long selfammount; //자납액
    private Long loanselfsum; //합계
    private Long loanselfcurrent; //잔액

    @OneToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
