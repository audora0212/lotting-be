package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.time.LocalDate;

@Embeddable
@Data
public class Meetingattend {
    private String howtoattend; //참석방법 ( 나중에 셀 합칠 때 대비 )
    private Boolean ftofattend; //서면
    private Boolean selfattend; //직접
    private Boolean behalfattend; //대리
}
