package com.audora.lotting_be.model.customer.minor;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class MGM {
    private Long mgmfee; //수수료
    private String mgmcompanyname; //업체명
    private String mgmname; //이름
    private String mgminstitution; //기관
    private String mgmaccount; //계좌
}