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
    private String checklist; //체크리스트
    private Boolean contract; //지산 A동 계약서
    private Boolean agreement; //동의서

    @Embedded
    private CustomerData customerData; //가입자

    @Embedded
    private LegalAddress legalAddress; //법정주소

    @Embedded
    private Financial financial; //금융기관

    @Embedded
    private Deposit deposits; //예약금

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Phase> phases; //n차

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    private Loan loan; //대출,자납

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    private Status status; //현 상태

    @Embedded
    private Responsible responsible;
}
