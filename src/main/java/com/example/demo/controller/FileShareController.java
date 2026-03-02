package com.example.demo.controller;

import com.example.demo.dto.FileDTO;
import com.example.demo.dto.FileShareDTO;
import com.example.demo.dto.ShareFileRequest;
import com.example.demo.service.FileShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shares")
@RequiredArgsConstructor
@Slf4j
public class FileShareController {
    
    private final FileShareService fileShareService;
    
    @PostMapping
    public ResponseEntity<FileShareDTO> shareFile(
            @RequestBody ShareFileRequest request,
            Authentication authentication) {
        log.info("POST /api/shares - User: {}, File: {}", 
                authentication.getName(), request.getFileId());
        
        FileShareDTO share = fileShareService.shareFile(request, authentication.getName());
        return ResponseEntity.ok(share);
    }
    
    @GetMapping("/shared-with-me")
    public ResponseEntity<List<FileDTO>> getSharedWithMe(Authentication authentication) {
        log.info("GET /api/shares/shared-with-me - User: {}", authentication.getName());
        
        List<FileDTO> files = fileShareService.getSharedWithMe(authentication.getName());
        return ResponseEntity.ok(files);
    }
    
    @GetMapping("/file/{fileId}")
    public ResponseEntity<List<FileShareDTO>> getFileShares(
            @PathVariable Long fileId,
            Authentication authentication) {
        log.info("GET /api/shares/file/{} - User: {}", fileId, authentication.getName());
        
        List<FileShareDTO> shares = fileShareService.getFileShares(fileId, authentication.getName());
        return ResponseEntity.ok(shares);
    }
    
    @DeleteMapping("/{shareId}")
    public ResponseEntity<Void> unshareFile(
            @PathVariable Long shareId,
            Authentication authentication) {
        log.info("DELETE /api/shares/{} - User: {}", shareId, authentication.getName());
        
        fileShareService.unshareFile(shareId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
