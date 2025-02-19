package com.audora.lotting_be.model.customer.minor;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.time.LocalDate;

@Embeddable
@Data
public class Cancel {

    private LocalDate canceldate; //해지일자
    private LocalDate refunddate; //환급일자
    private Integer refundamount; //환급금
}