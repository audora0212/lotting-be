// Loan.java
package com.audora.lotting_be.model.customer.minor;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Embeddable
@Data
public class Loan {
    private LocalDate loandate; //대출일자
    private String loanbank; //은행
    private Long loanammount; //대출액
    private LocalDate selfdate; //자납일
    private Long selfammount; //자납액
    private Long loanselfsum; //합계
    private Long loanselfcurrent; //잔액
}
