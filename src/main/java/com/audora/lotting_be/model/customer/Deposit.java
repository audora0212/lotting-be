// Deposit.java
package com.audora.lotting_be.model.customer;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Embeddable
@Data
public class Deposit {
    private LocalDate depositdate; //납입일자
    private Long depositammount; //금액

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
