package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyAuthenticationRequest {
    private String credentialId;
    private String authenticatorData;
    private String clientDataJSON;
    private String signature;
    private String userHandle;
}
