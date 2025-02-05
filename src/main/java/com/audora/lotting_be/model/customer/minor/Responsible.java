package com.audora.lotting_be.model.customer.minor;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Responsible {
    private String generalmanagement; //총괄
    private String division; //본부
    private String team; //팀
    private String managername; //담당자 성명
}