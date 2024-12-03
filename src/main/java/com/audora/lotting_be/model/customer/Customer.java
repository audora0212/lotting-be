// Customer.java
package com.audora.lotting_be.model.customer;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; //관리번호

    private String type; //타입
    private String groupname; //군
    private Integer turn; //순번
    private String batch; //가입차순
    private LocalDate registerdate; //가입일자
    private Long registerprice; //가입가

    @Embedded
    private CustomerData customerData; //가입자

    @Embedded
    private LegalAddress legalAddress; //법정주소

    @Embedded
    private Postreceive postreceive; //우편물 수령주소

    @Embedded
    private Financial financial; //금융기관

    @Embedded
    private Deposit deposits; //예약금

    @Embedded
    private Attachments attachments; //제출서류

    @Embedded
    private Loan loan; //대출,자납

    @Embedded
    private Responsible responsible;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Phase> phases; //n차

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Status status; //현 상태


}
