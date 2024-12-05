// src/main/java/com/audora/lotting_be/payload/request/SignupRequest.java
package com.audora.lotting_be.payload.request;

import lombok.Data;
import java.util.Set;

@Data
public class SignupRequest {
    private String username;
    private String email; // 필요시 추가
    private String password;
    private Set<String> roles;
}
