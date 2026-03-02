package com.example.demo.controller;

import com.example.demo.dto.FileDTO;
import com.example.demo.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    
    private final FileStorageService fileStorageService;
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderPath", required = false) String folderPath,
            Authentication authentication) {
        
        log.info("POST /api/files - Uploading file: {} by user: {} to folder: {}", 
                file.getOriginalFilename(), authentication.getName(), folderPath);
        
        FileDTO fileDTO = fileStorageService.uploadFile(file, authentication.getName(), folderPath);
        return ResponseEntity.status(HttpStatus.CREATED).body(fileDTO);
    }
    
    @GetMapping
    public ResponseEntity<List<FileDTO>> getUserFiles(Authentication authentication) {
        log.info("GET /api/files - Fetching files for user: {}", authentication.getName());
        
        List<FileDTO> files = fileStorageService.getUserFiles(authentication.getName());
        return ResponseEntity.ok(files);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<FileDTO> getFileInfo(
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("GET /api/files/{} - Getting file info by user: {}", id, authentication.getName());
        
        FileDTO fileDTO = fileStorageService.getFileInfo(id, authentication.getName());
        return ResponseEntity.ok(fileDTO);
    }
    
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("GET /api/files/{}/download - Downloading file by user: {}", 
                id, authentication.getName());
        
        Resource resource = fileStorageService.downloadFile(id, authentication.getName());
        FileDTO fileInfo = fileStorageService.getFileInfo(id, authentication.getName());
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileInfo.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + fileInfo.getOriginalFileName() + "\"")
                .body(resource);
    }
    
    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> previewFile(
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("GET /api/files/{}/preview - Previewing file by user: {}", 
                id, authentication.getName());
        
        Resource resource = fileStorageService.downloadFile(id, authentication.getName());
        FileDTO fileInfo = fileStorageService.getFileInfo(id, authentication.getName());
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileInfo.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }
    
    @PostMapping("/{id}/trash")
    public ResponseEntity<Void> moveToTrash(
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("POST /api/files/{}/trash - Moving to trash by user: {}", id, authentication.getName());
        
        fileStorageService.moveToTrash(id, authentication.getName());
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/trash")
    public ResponseEntity<List<FileDTO>> getTrashFiles(Authentication authentication) {
        log.info("GET /api/files/trash - Fetching trash files for user: {}", authentication.getName());
        
        List<FileDTO> files = fileStorageService.getTrashFiles(authentication.getName());
        return ResponseEntity.ok(files);
    }
    
    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restoreFile(
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("POST /api/files/{}/restore - Restoring file by user: {}", id, authentication.getName());
        
        fileStorageService.restoreFile(id, authentication.getName());
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("DELETE /api/files/{} - Deleting file permanently by user: {}", id, authentication.getName());
        
        fileStorageService.deleteFile(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
