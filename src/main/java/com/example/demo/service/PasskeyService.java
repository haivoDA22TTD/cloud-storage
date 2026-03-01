package com.example.demo.service;

import com.example.demo.dto.PasskeyAuthenticationRequest;
import com.example.demo.dto.PasskeyCredentialDTO;
import com.example.demo.dto.PasskeyRegistrationRequest;
import com.example.demo.entity.PasskeyCredential;
import com.example.demo.entity.User;
import com.example.demo.exception.PasskeyException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.PasskeyCredentialRepository;
import com.example.demo.repository.UserRepository;
import com.yubico.webauthn.data.ByteArray;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasskeyService {
    
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final UserRepository userRepository;
    
    // Store challenges temporarily (in production, use Redis or similar)
    private final Map<String, String> registrationChallenges = new HashMap<>();
    private final Map<String, String> authenticationChallenges = new HashMap<>();
    
    public Map<String, Object> startRegistration(String username, String rpId) {
        log.info("Starting passkey registration for user: {} with RP ID: {}", username, rpId);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        // Generate challenge
        String challenge = generateChallenge();
        registrationChallenges.put(username, challenge);
        
        Map<String, Object> options = new HashMap<>();
        options.put("challenge", challenge);
        options.put("rp", Map.of(
            "name", "Demo App",
            "id", rpId
        ));
        options.put("user", Map.of(
            "id", Base64.getEncoder().encodeToString(user.getId().toString().getBytes()),
            "name", user.getUsername(),
            "displayName", user.getUsername()
        ));
        options.put("pubKeyCredParams", List.of(
            Map.of("type", "public-key", "alg", -7),  // ES256
            Map.of("type", "public-key", "alg", -257) // RS256
        ));
        options.put("timeout", 60000);
        options.put("attestation", "none");
        options.put("authenticatorSelection", Map.of(
            "authenticatorAttachment", "platform",
            "requireResidentKey", false,
            "userVerification", "preferred"
        ));
        
        return options;
    }
    
    @Transactional
    public PasskeyCredentialDTO finishRegistration(String username, PasskeyRegistrationRequest request) {
        log.info("Finishing passkey registration for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        // Verify challenge
        String expectedChallenge = registrationChallenges.get(username);
        if (expectedChallenge == null) {
            throw new PasskeyException("No registration challenge found");
        }
        
        // In production, verify attestation and signature here
        // For simplicity, we'll just store the credential
        
        PasskeyCredential credential = PasskeyCredential.builder()
                .credentialId(request.getCredentialId())
                .publicKey(request.getPublicKey())
                .signatureCount(0L)
                .aaguid("00000000-0000-0000-0000-000000000000")
                .credentialName(request.getCredentialName())
                .user(user)
                .build();
        
        PasskeyCredential saved = passkeyCredentialRepository.save(credential);
        registrationChallenges.remove(username);
        
        log.info("Passkey registered successfully for user: {}", username);
        return convertToDTO(saved);
    }
    
    public Map<String, Object> startAuthentication(String username, String rpId) {
        log.info("Starting passkey authentication for user: {} with RP ID: {}", username, rpId);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        List<PasskeyCredential> credentials = passkeyCredentialRepository.findByUser(user);
        if (credentials.isEmpty()) {
            throw new PasskeyException("No passkeys registered for this user");
        }
        
        // Generate challenge
        String challenge = generateChallenge();
        authenticationChallenges.put(username, challenge);
        
        List<Map<String, String>> allowCredentials = credentials.stream()
                .map(cred -> Map.of(
                    "type", "public-key",
                    "id", cred.getCredentialId()
                ))
                .collect(Collectors.toList());
        
        Map<String, Object> options = new HashMap<>();
        options.put("challenge", challenge);
        options.put("timeout", 60000);
        options.put("rpId", rpId);
        options.put("allowCredentials", allowCredentials);
        options.put("userVerification", "preferred");
        
        return options;
    }
    
    @Transactional
    public User finishAuthentication(String username, PasskeyAuthenticationRequest request) {
        log.info("Finishing passkey authentication for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        // Verify challenge
        String expectedChallenge = authenticationChallenges.get(username);
        if (expectedChallenge == null) {
            throw new PasskeyException("No authentication challenge found");
        }
        
        // Find credential
        PasskeyCredential credential = passkeyCredentialRepository.findByCredentialId(request.getCredentialId())
                .orElseThrow(() -> new PasskeyException("Credential not found"));
        
        // Verify ownership
        if (!credential.getUser().getId().equals(user.getId())) {
            throw new PasskeyException("Credential does not belong to this user");
        }
        
        // In production, verify signature here using the public key
        // For simplicity, we'll just update last used time
        
        credential.setLastUsedAt(LocalDateTime.now());
        credential.setSignatureCount(credential.getSignatureCount() + 1);
        passkeyCredentialRepository.save(credential);
        
        authenticationChallenges.remove(username);
        
        log.info("Passkey authentication successful for user: {}", username);
        return user;
    }
    
    @Transactional(readOnly = true)
    public List<PasskeyCredentialDTO> getUserPasskeys(String username) {
        log.debug("Fetching passkeys for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        return passkeyCredentialRepository.findByUser(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void deletePasskey(String username, Long passkeyId) {
        log.info("Deleting passkey {} for user: {}", passkeyId, username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        PasskeyCredential credential = passkeyCredentialRepository.findById(passkeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Passkey not found"));
        
        if (!credential.getUser().getId().equals(user.getId())) {
            throw new PasskeyException("Passkey does not belong to this user");
        }
        
        passkeyCredentialRepository.delete(credential);
        log.info("Passkey deleted successfully");
    }
    
    @Transactional(readOnly = true)
    public boolean hasPasskey(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return passkeyCredentialRepository.existsByUser(user);
    }
    
    private String generateChallenge() {
        byte[] bytes = new byte[32];
        new Random().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private PasskeyCredentialDTO convertToDTO(PasskeyCredential credential) {
        return PasskeyCredentialDTO.builder()
                .id(credential.getId())
                .credentialId(credential.getCredentialId())
                .credentialName(credential.getCredentialName())
                .createdAt(credential.getCreatedAt())
                .lastUsedAt(credential.getLastUsedAt())
                .build();
    }
}
