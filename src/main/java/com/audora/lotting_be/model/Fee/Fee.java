// Fee.java
package com.audora.lotting_be.model.Fee;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Fee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupname;
    private String floor;
    private String batch;
    private String type;
    private Long supplyarea;
    private Long priceperp;
    private Long price;
    private Double paymentratio;
    private Long paysum;

    @OneToMany(mappedBy = "fee", cascade = CascadeType.ALL)
    private List<FeePerPhase> feePerPhases;
}
