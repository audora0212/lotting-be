// Attachments.java
package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Attachments {
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
