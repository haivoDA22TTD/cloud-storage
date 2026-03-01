package com.example.demo.controller;

import com.example.demo.dto.PasskeyAuthenticationRequest;
import com.example.demo.dto.PasskeyCredentialDTO;
import com.example.demo.dto.PasskeyRegistrationRequest;
import com.example.demo.entity.User;
import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.PasskeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/passkey")
@RequiredArgsConstructor
@Slf4j
public class PasskeyController {
    
    private final PasskeyService passkeyService;
    private final CustomUserDetailsService customUserDetailsService;
    
    @PostMapping("/register/start")
    public ResponseEntity<Map<String, Object>> startRegistration(Authentication authentication) {
        log.info("POST /api/passkey/register/start - User: {}", authentication.getName());
        Map<String, Object> options = passkeyService.startRegistration(authentication.getName());
        return ResponseEntity.ok(options);
    }
    
    @PostMapping("/register/finish")
    public ResponseEntity<PasskeyCredentialDTO> finishRegistration(
            @RequestBody PasskeyRegistrationRequest request,
            Authentication authentication) {
        log.info("POST /api/passkey/register/finish - User: {}", authentication.getName());
        PasskeyCredentialDTO credential = passkeyService.finishRegistration(authentication.getName(), request);
        return ResponseEntity.ok(credential);
    }
    
    @PostMapping("/authenticate/start")
    public ResponseEntity<Map<String, Object>> startAuthentication(@RequestParam String username) {
        log.info("POST /api/passkey/authenticate/start - User: {}", username);
        Map<String, Object> options = passkeyService.startAuthentication(username);
        return ResponseEntity.ok(options);
    }
    
    @PostMapping("/authenticate/finish")
    public ResponseEntity<Map<String, String>> finishAuthentication(
            @RequestBody PasskeyAuthenticationRequest request,
            @RequestParam String username,
            HttpServletRequest httpRequest) {
        log.info("POST /api/passkey/authenticate/finish - User: {}", username);
        
        User user = passkeyService.finishAuthentication(username, request);
        
        // Create Spring Security authentication and session
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        
        // Save to session
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
        
        log.info("Session created for user: {}", username);
        
        return ResponseEntity.ok(Map.of(
            "username", user.getUsername(), 
            "status", "success",
            "sessionId", session.getId()
        ));
    }
    
    @GetMapping
    public ResponseEntity<List<PasskeyCredentialDTO>> getUserPasskeys(Authentication authentication) {
        log.info("GET /api/passkey - User: {}", authentication.getName());
        List<PasskeyCredentialDTO> passkeys = passkeyService.getUserPasskeys(authentication.getName());
        return ResponseEntity.ok(passkeys);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePasskey(
            @PathVariable Long id,
            Authentication authentication) {
        log.info("DELETE /api/passkey/{} - User: {}", id, authentication.getName());
        passkeyService.deletePasskey(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/has-passkey")
    public ResponseEntity<Map<String, Boolean>> hasPasskey(Authentication authentication) {
        boolean hasPasskey = passkeyService.hasPasskey(authentication.getName());
        return ResponseEntity.ok(Map.of("hasPasskey", hasPasskey));
    }
}
