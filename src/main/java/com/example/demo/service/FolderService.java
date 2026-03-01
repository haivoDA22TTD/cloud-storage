package com.example.demo.service;

import com.example.demo.dto.CreateFolderRequest;
import com.example.demo.dto.FolderDTO;
import com.example.demo.entity.Folder;
import com.example.demo.entity.User;
import com.example.demo.exception.AccessDeniedException;
import com.example.demo.exception.ResourceAlreadyExistsException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FolderService {
    
    private static final String BASE_UPLOAD_DIR = "data";
    
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public FolderDTO createFolder(String username, CreateFolderRequest request) {
        log.info("Creating folder: {} for user: {}", request.getName(), username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        // Validate folder name
        if (request.getName().contains("..") || request.getName().contains("/") || request.getName().contains("\\")) {
            throw new IllegalArgumentException("Invalid folder name");
        }
        
        // Build folder path
        String folderPath;
        Folder parent = null;
        
        if (request.getParentId() != null) {
            parent = folderRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent folder not found"));
            
            if (!parent.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException("You don't have permission to create folder here");
            }
            
            folderPath = parent.getPath() + "/" + request.getName();
        } else {
            folderPath = request.getName();
        }
        
        // Check if folder already exists
        if (folderRepository.existsByPathAndOwner(folderPath, user)) {
            throw new ResourceAlreadyExistsException("Folder already exists: " + folderPath);
        }
        
        // Create physical directory
        Path physicalPath = Paths.get(BASE_UPLOAD_DIR, user.getId().toString(), folderPath);
        try {
            Files.createDirectories(physicalPath);
            log.info("Physical directory created: {}", physicalPath);
        } catch (IOException e) {
            log.error("Failed to create directory: {}", physicalPath, e);
            throw new RuntimeException("Failed to create folder", e);
        }
        
        // Save folder metadata
        Folder folder = Folder.builder()
                .name(request.getName())
                .path(folderPath)
                .parent(parent)
                .owner(user)
                .build();
        
        Folder savedFolder = folderRepository.save(folder);
        log.info("Folder created successfully: {}", savedFolder.getId());
        
        return convertToDTO(savedFolder);
    }
    
    @Transactional(readOnly = true)
    public List<FolderDTO> getUserFolders(String username) {
        log.debug("Fetching folders for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        return folderRepository.findByOwner(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void deleteFolder(String username, Long folderId) {
        log.info("Deleting folder {} for user: {}", folderId, username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        
        if (!folder.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have permission to delete this folder");
        }
        
        // Delete physical directory
        Path physicalPath = Paths.get(BASE_UPLOAD_DIR, user.getId().toString(), folder.getPath());
        try {
            Files.deleteIfExists(physicalPath);
            log.info("Physical directory deleted: {}", physicalPath);
        } catch (IOException e) {
            log.error("Failed to delete directory: {}", physicalPath, e);
        }
        
        folderRepository.delete(folder);
        log.info("Folder deleted successfully");
    }
    
    private FolderDTO convertToDTO(Folder folder) {
        return FolderDTO.builder()
                .id(folder.getId())
                .name(folder.getName())
                .path(folder.getPath())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .createdAt(folder.getCreatedAt())
                .build();
    }
}
