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

    private String groupname; //군
    private String floor; //층
    private String batch; //가입차순
    private String type; //타입
    private Long supplyarea; //공급면적
    private Long priceperp; //평당가
    private Long price; //금액
    private Double paymentratio; //납입비율
    private Long paysum; //합계

    @OneToMany(mappedBy = "fee", cascade = CascadeType.ALL)
    private List<FeePerPhase> feePerPhases;
}
