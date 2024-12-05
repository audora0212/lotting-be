// src/main/java/com/audora/lotting_be/payload/response/MessageResponse.java
package com.audora.lotting_be.payload.response;

import lombok.Data;

@Data
public class MessageResponse {
    private String message;

    public MessageResponse(String message) {
        this.message = message;
    }
}
