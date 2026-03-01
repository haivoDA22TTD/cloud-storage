package com.example.demo.service;

import com.example.demo.dto.FileDTO;
import com.example.demo.entity.FileEntity;
import com.example.demo.entity.User;
import com.example.demo.exception.AccessDeniedException;
import com.example.demo.exception.FileStorageException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.FileRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    
    private static final String BASE_UPLOAD_DIR = "data";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public FileDTO uploadFile(MultipartFile file, String username, String folderPath) {
        log.info("Uploading file: {} for user: {} to folder: {}", file.getOriginalFilename(), username, folderPath);
        
        // Validate file
        validateFile(file);
        
        // Get user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        // Generate unique filename
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        
        // Create user directory with folder path
        Path userDirectory;
        if (folderPath != null && !folderPath.isEmpty()) {
            userDirectory = Paths.get(BASE_UPLOAD_DIR, user.getId().toString(), folderPath);
        } else {
            userDirectory = Paths.get(BASE_UPLOAD_DIR, user.getId().toString());
        }
        
        try {
            Files.createDirectories(userDirectory);
        } catch (IOException e) {
            log.error("Failed to create directory: {}", userDirectory, e);
            throw new FileStorageException("Failed to create upload directory", e);
        }
        
        // Save file to disk
        Path filePath = userDirectory.resolve(uniqueFileName);
        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved to: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to save file: {}", filePath, e);
            throw new FileStorageException("Failed to save file", e);
        }
        
        // Save metadata to database
        FileEntity fileEntity = FileEntity.builder()
                .fileName(uniqueFileName)
                .originalFileName(originalFileName)
                .filePath(filePath.toString())
                .folderPath(folderPath)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .isDeleted(false)
                .owner(user)
                .build();
        
        FileEntity savedFile = fileRepository.save(fileEntity);
        log.info("File metadata saved with id: {}", savedFile.getId());
        
        return convertToDTO(savedFile);
    }
    
    @Transactional(readOnly = true)
    public List<FileDTO> getUserFiles(String username) {
        log.debug("Fetching files for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        return fileRepository.findByOwner(user).stream()
                .filter(f -> !f.getIsDeleted())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void moveToTrash(Long fileId, String username) {
        log.info("Moving file {} to trash for user: {}", fileId, username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        
        if (!fileEntity.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have permission to delete this file");
        }
        
        fileEntity.setIsDeleted(true);
        fileEntity.setDeletedAt(java.time.LocalDateTime.now());
        fileRepository.save(fileEntity);
        
        log.info("File moved to trash: {}", fileId);
    }
    
    @Transactional(readOnly = true)
    public List<FileDTO> getTrashFiles(String username) {
        log.debug("Fetching trash files for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        return fileRepository.findByOwner(user).stream()
                .filter(FileEntity::getIsDeleted)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void restoreFile(Long fileId, String username) {
        log.info("Restoring file {} for user: {}", fileId, username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        
        if (!fileEntity.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have permission to restore this file");
        }
        
        fileEntity.setIsDeleted(false);
        fileEntity.setDeletedAt(null);
        fileRepository.save(fileEntity);
        
        log.info("File restored: {}", fileId);
    }
    
    @Transactional(readOnly = true)
    public Resource downloadFile(Long fileId, String username) {
        log.info("Downloading file id: {} for user: {}", fileId, username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        
        // Check ownership
        if (!fileEntity.getOwner().getId().equals(user.getId())) {
            log.warn("User {} attempted to access file {} owned by {}", 
                    username, fileId, fileEntity.getOwner().getUsername());
            throw new AccessDeniedException("You don't have permission to access this file");
        }
        
        try {
            Path filePath = Paths.get(fileEntity.getFilePath()).normalize();
            
            // Prevent path traversal
            if (!filePath.startsWith(Paths.get(BASE_UPLOAD_DIR, user.getId().toString()))) {
                log.error("Path traversal attempt detected: {}", filePath);
                throw new AccessDeniedException("Invalid file path");
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                log.error("File not found or not readable: {}", filePath);
                throw new ResourceNotFoundException("File not found or not readable");
            }
        } catch (IOException e) {
            log.error("Error loading file: {}", fileEntity.getFilePath(), e);
            throw new FileStorageException("Error loading file", e);
        }
    }
    
    @Transactional
    public void deleteFile(Long fileId, String username) {
        log.info("Deleting file id: {} for user: {}", fileId, username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        
        // Check ownership
        if (!fileEntity.getOwner().getId().equals(user.getId())) {
            log.warn("User {} attempted to delete file {} owned by {}", 
                    username, fileId, fileEntity.getOwner().getUsername());
            throw new AccessDeniedException("You don't have permission to delete this file");
        }
        
        // Delete file from disk
        try {
            Path filePath = Paths.get(fileEntity.getFilePath()).normalize();
            Files.deleteIfExists(filePath);
            log.info("File deleted from disk: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to delete file from disk: {}", fileEntity.getFilePath(), e);
            throw new FileStorageException("Failed to delete file from disk", e);
        }
        
        // Delete metadata from database
        fileRepository.delete(fileEntity);
        log.info("File metadata deleted with id: {}", fileId);
    }
    
    @Transactional(readOnly = true)
    public FileDTO getFileInfo(Long fileId, String username) {
        log.debug("Getting file info for id: {} by user: {}", fileId, username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        
        // Check ownership
        if (!fileEntity.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have permission to access this file");
        }
        
        return convertToDTO(fileEntity);
    }
    
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot upload empty file");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File size exceeds maximum limit of 50MB");
        }
        
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.contains("..")) {
            throw new FileStorageException("Invalid file name");
        }
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : fileName.substring(lastDotIndex);
    }
    
    private FileDTO convertToDTO(FileEntity fileEntity) {
        return FileDTO.builder()
                .id(fileEntity.getId())
                .fileName(fileEntity.getFileName())
                .originalFileName(fileEntity.getOriginalFileName())
                .contentType(fileEntity.getContentType())
                .fileSize(fileEntity.getFileSize())
                .fileSizeFormatted(formatFileSize(fileEntity.getFileSize()))
                .folderPath(fileEntity.getFolderPath())
                .isDeleted(fileEntity.getIsDeleted())
                .ownerId(fileEntity.getOwner().getId())
                .ownerUsername(fileEntity.getOwner().getUsername())
                .uploadedAt(fileEntity.getUploadedAt())
                .deletedAt(fileEntity.getDeletedAt())
                .build();
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z));
    }
}
