package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class LegalAddress {
    private Integer postnumber; //우편번호
    private String province; //도
    private String county; //군
    private String detailaddress; //상세주소
}