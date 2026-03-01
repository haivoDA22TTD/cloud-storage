package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyCredentialDTO {
    private Long id;
    private String credentialId;
    private String credentialName;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
