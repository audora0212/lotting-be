package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.time.LocalDate;

@Embeddable
@Data
public class Secondemp {
    private String secondemptimes; //차순
    private LocalDate secondempdate; //지급일자
}
