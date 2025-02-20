package com.audora.lotting_be.model.customer.minor;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.time.LocalDate;

@Embeddable
@Data
public class Financial {
    private String bankname; //은행명
    private String accountnum; //계좌번호
    private String accountholder; //예금주
    private String trustcompany; //신탁사
    private LocalDate trustcompanydate; //신탁사제출일자
}