package com.audora.lotting_be.model.customer.minor;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class CustomerData {
    private String name; //성명
    private Integer resnumfront; //주민번호 앞자리
    private Integer resnumback; //주민번호 뒷자리
    private String phone; //휴대전화
    private String email; // E-mail
}