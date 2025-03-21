// src/main/java/com/audora/lotting_be/model/customer/Customer.java
package com.audora.lotting_be.model.customer;

import com.audora.lotting_be.model.customer.minor.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Entity
@Data
@EqualsAndHashCode(exclude = "status")
public class Customer {
    @Id
    private Integer id; // 관리번호

    private String customertype; // 분류(회원)
    private String type;         // 타입
    private String groupname;    // 군
    private String turn;         // 순번
    private String temptype;     //임시동호
    private String batch;        // 가입차순
    private LocalDate registerdate; // 가입일자
    private Long registerprice;     // 가입가
    private String additional;      // 비고
    private String registerpath;    // 가입경로
    private String specialnote;     // 특이사항
    private String prizewinning;    // 경품당첨
    private String votemachine; //투표기기

    @Embedded
    private CustomerData customerData = new CustomerData(); // 가입자

    @Embedded
    private LegalAddress legalAddress = new LegalAddress();   // 법정주소

    @Embedded
    private Postreceive postreceive = new Postreceive();      // 우편물 수령주소

    @Embedded
    private Financial financial = new Financial();            // 금융기관

    @Embedded
    private Deposit deposits = new Deposit();                 // 예약금

    @Embedded
    private Attachments attachments = new Attachments();        // 부속서류

    @Embedded
    private Cancel cancel=new Cancel();                       //해약

    // 기존 대출/자납 기록용 loan 필드
    @Embedded
    private Loan loan = new Loan();                           // 대출, 자납

    @Embedded
    private Responsible responsible = new Responsible();      // 담당

    @Embedded
    private Dahim dahim = new Dahim();                        // 다힘

    @Embedded
    private MGM mgm = new MGM();                              // MGM

    @Embedded
    private Firstemp firstemp = new Firstemp();               // 1차(직원)

    @Embedded
    private Secondemp secondemp = new Secondemp();            // 2차

    @Embedded
    private Meetingattend meetingattend = new Meetingattend();  // 총회참석여부

    @Embedded
    private Agenda agenda = new Agenda();        // 안건

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Phase> phases; // n차

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Status status; // 현 상태

    // 여러 입금내역
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonManagedReference(value = "customer-depositHistories")
    private List<DepositHistory> depositHistories;

    @Transient
    private Map<String, Object> cancelInfo;

    public Map<String, Object> getCancelInfo() {
        return cancelInfo;
    }

    public void setCancelInfo(Map<String, Object> cancelInfo) {
        this.cancelInfo = cancelInfo;
    }
}
