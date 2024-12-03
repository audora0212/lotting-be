package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Financial {
    private String bankname; //은행명
    private String accountnum; //계좌번호
    private String accountholder; //예금주
    private String trustcompany; //신탁사
}