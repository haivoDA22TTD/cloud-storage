package com.example.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class FileViewController {
    
    @GetMapping("/files")
    public String filesPage() {
        return "files-simple";
    }
}
