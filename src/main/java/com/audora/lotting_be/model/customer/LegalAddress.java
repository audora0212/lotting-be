// src/main/java/com/audora/lotting_be/model/customer/LegalAddress.java
package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class LegalAddress {
    private Integer postnumber; //우편번호
    private String post; //주소
    private String detailaddress; //상세주소
}

