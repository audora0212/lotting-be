package com.audora.lotting_be.model.customer.minor;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Meetingattend {
    private String ftofattend;     // 서면
    private String selfattend;     // 직접
    private String behalfattend;   // 대리
}
