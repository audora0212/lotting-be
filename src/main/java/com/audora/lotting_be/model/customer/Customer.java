// Customer.java
package com.audora.lotting_be.model.customer;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String type;
    private String groupname;
    private Integer turn;
    private String batch;
    private LocalDate registerdate;
    private Long registerprice;
    private String checklist;
    private Boolean contract;
    private Boolean agreement;

    @Embedded
    private CustomerData customerData;

    @Embedded
    private LegalAddress legalAddress;

    @Embedded
    private Financial financial;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Deposit> deposits;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Phase> phases;

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    private Loan loan;

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    private Status status;

    @Embedded
    private Responsible responsible;
}
