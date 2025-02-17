package com.audora.lotting_be.model.customer.minor;
import com.audora.lotting_be.model.customer.Customer;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Embeddable
@Data
public class Agenda {

    private String agenda1;     // 제1호
    private String agenda2_1;   // 제2-1호
    private String agenda2_2;   // 제2-2호
    private String agenda2_3;   // 제2-3호
    private String agenda2_4;   // 제2-4호
    private String agenda3;     // 제3호
    private String agenda4;     // 제4호
    private String agenda5;     // 제5호
    private String agenda6;     // 제6호
    private String agenda7;     // 제7호
    private String agenda8;     // 제8호
    private String agenda9;     // 제9호
    private String agenda10;    // 제10호

    @OneToOne
    @JoinColumn(name = "customer_id")
    @JsonBackReference
    private Customer customer;
}
