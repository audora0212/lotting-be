package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Financial {
    private String bankname;
    private Long accountnum;
    private String accountholder;
    private String trustcompany;
}