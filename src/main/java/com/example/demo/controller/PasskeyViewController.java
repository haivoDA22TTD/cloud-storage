package com.example.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PasskeyViewController {
    
    @GetMapping("/passkey-setup")
    public String passkeySetupPage() {
        return "passkey-setup";
    }
    
    @GetMapping("/passkey-login")
    public String passkeyLoginPage() {
        return "passkey-login";
    }
}
