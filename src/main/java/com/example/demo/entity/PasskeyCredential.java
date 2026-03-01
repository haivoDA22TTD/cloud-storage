package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "passkey_credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyCredential {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String credentialId;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String publicKey;
    
    @Column(nullable = false)
    private Long signatureCount;
    
    @Column(nullable = false)
    private String aaguid;
    
    @Column(nullable = false)
    private String credentialName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime lastUsedAt;
}
