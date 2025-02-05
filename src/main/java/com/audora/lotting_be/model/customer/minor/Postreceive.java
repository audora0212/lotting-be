// src/main/java/com/audora/lotting_be/model/customer/Postreceive.java
package com.audora.lotting_be.model.customer.minor;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Postreceive {
    private Integer postnumberreceive; //우편번호
    private String postreceive; //주소
    private String detailaddressreceive; //상세주소
}
