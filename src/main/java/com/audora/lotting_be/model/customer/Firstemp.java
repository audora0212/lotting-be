package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.time.LocalDate;

@Embeddable
@Data
public class Firstemp {
    private String firstemptimes; //차순
    private LocalDate firstempdate; //지급일자
}
