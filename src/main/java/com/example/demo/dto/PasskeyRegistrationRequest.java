package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyRegistrationRequest {
    private String credentialName;
    private String credentialId;
    private String publicKey;
    private String attestationObject;
    private String clientDataJSON;
}
