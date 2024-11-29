// Deposit.java
package com.audora.lotting_be.model.customer;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Deposit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate depositdate;
    private Long depositammount;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
