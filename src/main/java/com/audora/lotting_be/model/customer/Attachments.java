// Attachments.java
package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Attachments {
    private Boolean isuploaded; // 파일 제출했는지 여부
    private String fileinfo; // 파일 주소, 이름, 확장자

    private Boolean exemption7; // 7차 면제
    private Boolean investmentfile; //출자금
    private Boolean contract; //지산 A동 계약서
    private Boolean agreement; //총회동의서

    private Boolean sealcertificateprovided; // 인감증명서 제출 여부
    private Boolean selfsignatureconfirmationprovided; // 본인서명확인서 제출 여부
    private Boolean idcopyprovided; // 신분증 사본 제출 여부
    private Boolean commitmentletterprovided; // 확약서 제출 여부
    private Boolean forfounding; //창준위용 제출여부
    private Boolean freeoption; //무상옵션 제출여부
    private Boolean preferenceattachment; //선호도조사 제출여부
    private Boolean generalmeetingconsentformprovided; // 총회 동의서 제출 여부
    private Boolean prizeattachment; // 사은품 제출 여부

}
