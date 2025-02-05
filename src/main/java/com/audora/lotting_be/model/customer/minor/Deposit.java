// src/main/java/com/audora/lotting_be/model/customer/Deposit.java

package com.audora.lotting_be.model.customer.minor;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.time.LocalDate;

@Embeddable
@Data
public class Deposit {
    private LocalDate depositdate; //납입일자
    private Long depositammount; //금액

}
