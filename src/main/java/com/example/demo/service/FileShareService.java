package com.example.demo.service;

import com.example.demo.dto.FileDTO;
import com.example.demo.dto.FileShareDTO;
import com.example.demo.dto.ShareFileRequest;
import com.example.demo.entity.FileEntity;
import com.example.demo.entity.FileShare;
import com.example.demo.entity.User;
import com.example.demo.exception.AccessDeniedException;
import com.example.demo.exception.ResourceAlreadyExistsException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.FileRepository;
import com.example.demo.repository.FileShareRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileShareService {
    
    private final FileShareRepository fileShareRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public FileShareDTO shareFile(ShareFileRequest request, String ownerUsername) {
        log.info("Sharing file {} with user {}", request.getFileId(), request.getSharedWithUsername());
        
        // Get file
        FileEntity file = fileRepository.findById(request.getFileId())
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        
        // Get owner
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));
        
        // Verify ownership
        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("You don't have permission to share this file");
        }
        
        // Get shared with user
        User sharedWith = userRepository.findByUsername(request.getSharedWithUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getSharedWithUsername()));
        
        // Check if already shared
        if (fileShareRepository.existsByFileAndSharedWith(file, sharedWith)) {
            throw new ResourceAlreadyExistsException("File already shared with this user");
        }
        
        // Cannot share with yourself
        if (owner.getId().equals(sharedWith.getId())) {
            throw new IllegalArgumentException("Cannot share file with yourself");
        }
        
        // Create share
        FileShare share = FileShare.builder()
                .file(file)
                .owner(owner)
                .sharedWith(sharedWith)
                .permission(request.getPermission() != null ? request.getPermission() : "view")
                .build();
        
        FileShare saved = fileShareRepository.save(share);
        
        log.info("File shared successfully: {}", saved.getId());
        return convertToDTO(saved);
    }
    
    @Transactional(readOnly = true)
    public List<FileDTO> getSharedWithMe(String username) {
        log.info("Getting files shared with user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<FileShare> shares = fileShareRepository.findBySharedWith(user);
        
        return shares.stream()
                .map(share -> convertFileToDTO(share.getFile(), share.getPermission()))
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<FileShareDTO> getFileShares(Long fileId, String ownerUsername) {
        log.info("Getting shares for file: {}", fileId);
        
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Verify ownership
        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("You don't have permission to view shares");
        }
        
        List<FileShare> shares = fileShareRepository.findByFile(file);
        
        return shares.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void unshareFile(Long shareId, String ownerUsername) {
        log.info("Unsharing file: {}", shareId);
        
        FileShare share = fileShareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Share not found"));
        
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Verify ownership
        if (!share.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("You don't have permission to unshare this file");
        }
        
        fileShareRepository.delete(share);
        log.info("File unshared successfully");
    }
    
    @Transactional(readOnly = true)
    public boolean canAccessFile(Long fileId, String username) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Owner can always access
        if (file.getOwner().getId().equals(user.getId())) {
            return true;
        }
        
        // Check if shared
        return fileShareRepository.existsByFileAndSharedWith(file, user);
    }
    
    private FileShareDTO convertToDTO(FileShare share) {
        return FileShareDTO.builder()
                .id(share.getId())
                .fileId(share.getFile().getId())
                .fileName(share.getFile().getFileName())
                .ownerUsername(share.getOwner().getUsername())
                .sharedWithUsername(share.getSharedWith().getUsername())
                .permission(share.getPermission())
                .sharedAt(share.getSharedAt())
                .build();
    }
    
    private FileDTO convertFileToDTO(FileEntity file, String permission) {
        return FileDTO.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .originalFileName(file.getOriginalFileName())
                .fileSize(file.getFileSize())
                .contentType(file.getContentType())
                .uploadedAt(file.getUploadedAt())
                .folderPath(file.getFolderPath())
                .isDeleted(file.getIsDeleted())
                .sharedPermission(permission)
                .build();
    }
}
