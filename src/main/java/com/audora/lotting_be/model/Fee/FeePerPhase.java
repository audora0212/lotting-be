// FeePerPhase.java
package com.audora.lotting_be.model.Fee;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class FeePerPhase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer phaseNumber;
    private Long phasefee;
    private String phasedate;

    @ManyToOne
    @JoinColumn(name = "fee_id")
    private Fee fee;
}
