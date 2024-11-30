package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Responsible {
    private String generalmanagement; //총괄
    private Integer division; //본부
    private Integer team; //팀
    private String managername; //담당자 성명
    private String registerroot; //가입경로
}