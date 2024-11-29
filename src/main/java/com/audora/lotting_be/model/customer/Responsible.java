package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Responsible {
    private String generalmanagement;
    private Integer division;
    private Integer team;
    private String managername;
    private String registerroot;
}