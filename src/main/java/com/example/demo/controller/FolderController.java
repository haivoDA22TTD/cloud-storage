package com.example.demo.controller;

import com.example.demo.dto.CreateFolderRequest;
import com.example.demo.dto.FolderDTO;
import com.example.demo.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
@Slf4j
public class FolderController {
    
    private final FolderService folderService;
    
    @PostMapping
    public ResponseEntity<FolderDTO> createFolder(
            @Valid @RequestBody CreateFolderRequest request,
            Authentication authentication) {
        
        log.info("POST /api/folders - Creating folder: {} by user: {}", 
                request.getName(), authentication.getName());
        
        FolderDTO folder = folderService.createFolder(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(folder);
    }
    
    @GetMapping
    public ResponseEntity<List<FolderDTO>> getUserFolders(Authentication authentication) {
        log.info("GET /api/folders - Fetching folders for user: {}", authentication.getName());
        
        List<FolderDTO> folders = folderService.getUserFolders(authentication.getName());
        return ResponseEntity.ok(folders);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("DELETE /api/folders/{} - Deleting folder by user: {}", id, authentication.getName());
        
        folderService.deleteFolder(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
