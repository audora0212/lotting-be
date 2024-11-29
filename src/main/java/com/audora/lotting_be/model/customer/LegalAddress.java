package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class LegalAddress {
    private Integer postnumber;
    private String province;
    private String county;
    private String detailaddress;
}