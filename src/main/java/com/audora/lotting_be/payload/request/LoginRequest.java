// src/main/java/com/audora/lotting_be/payload/request/LoginRequest.java
package com.audora.lotting_be.payload.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
