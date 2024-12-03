package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.time.LocalDate;

@Embeddable
@Data
public class Dahim {
    private String dahimsisang; //시상
    private LocalDate dahimdate; //일자
    private String dahimprepaid; //6/30선지금
    private String dahimfirst; //1회차청구
    private String dahimfirstpay; //(1회차?)금액
    private LocalDate dahimdate2; //일자2
    private String dahimsource; //출처
    private String dahimsecond; //2회차청구
    private String dahimsecondpay; //(2회차?)금액
    private LocalDate dahimdate3; //일자3
    private String dahimsum; //합계
}