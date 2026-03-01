package com.example.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class TrashViewController {
    
    @GetMapping("/trash")
    public String trashPage() {
        return "trash";
    }
}
