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

    private Integer phaseNumber; //n차
    private Long phasefee; //금액
    private String phasedate; //제출일

    @ManyToOne
    @JoinColumn(name = "fee_id")
    private Fee fee;
}
