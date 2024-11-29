package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class CustomerData {
    private String name;
    private Integer resnumfront;
    private Integer resnumback;
    private String phone;
}