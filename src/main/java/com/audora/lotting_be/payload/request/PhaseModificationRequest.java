package com.audora.lotting_be.payload.request;

import lombok.Data;

@Data
public class PhaseModificationRequest {
    // 수정 허용 필드
    private Long charge;      // 부담금
    private Long service;     // 업무대행비
    private Long discount;    // 할인액
    private Long exemption;   // 면제액
    private String move;      // 이동
}
