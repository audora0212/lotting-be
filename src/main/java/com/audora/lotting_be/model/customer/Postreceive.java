package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Postreceive {
    private Integer postnumberreceive; //우편번호
    private String provincereceive; //도
    private String countyreceive; //군
    private String detailaddressreceive; //상세주소
}