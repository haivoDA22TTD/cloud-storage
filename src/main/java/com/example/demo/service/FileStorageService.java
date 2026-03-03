package com.example.demo.service;

import com.example.demo.dto.FileDTO;
import com.example.demo.entity.FileEntity;
import com.example.demo.entity.User;
import com.example.demo.exception.AccessDeniedException;
import com.example.demo.exception.FileStorageException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.FileRepository;
import com.example.demo.repository.FileShareRepository;
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
import java.util.Map;
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
    private final FileShareRepository fileShareRepository;
    private final CloudinaryService cloudinaryService;
    
    @Transactional
    public FileDTO uploadFile(MultipartFile file, String username, String folderPath) {
        log.info("Uploading file: {} for user: {} to folder: {}", file.getOriginalFilename(), username, folderPath);
        
        // Validate file
        validateFile(file);
        
        // Get user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        String originalFileName = file.getOriginalFilename();
        
        try {
            // Upload to Cloudinary
            String cloudinaryFolder = "user_" + user.getId();
            if (folderPath != null && !folderPath.isEmpty()) {
                cloudinaryFolder += "/" + folderPath.replace("\\", "/");
            }
            
            Map<String, Object> uploadResult = cloudinaryService.uploadFile(file, cloudinaryFolder);
            
            String cloudinaryUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            
            log.info("File uploaded to Cloudinary. URL: {}, Public ID: {}", cloudinaryUrl, publicId);
            
            // Save metadata to database
            FileEntity fileEntity = FileEntity.builder()
                    .fileName((String) uploadResult.get("original_filename"))
                    .originalFileName(originalFileName)
                    .filePath(cloudinaryUrl) // Store URL for backward compatibility
                    .cloudinaryUrl(cloudinaryUrl)
                    .cloudinaryPublicId(publicId)
                    .folderPath(folderPath)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .isDeleted(false)
                    .owner(user)
                    .build();
            
            FileEntity savedFile = fileRepository.save(fileEntity);
            log.info("File metadata saved with id: {}", savedFile.getId());
            
            return convertToDTO(savedFile);
            
        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new FileStorageException("Failed to upload file to cloud storage", e);
        }
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
        
        // Check if owner
        boolean isOwner = fileEntity.getOwner().getId().equals(user.getId());
        
        // Check if has edit permission
        boolean hasEditPermission = fileShareRepository.findByFileAndSharedWith(fileEntity, user)
                .map(share -> "edit".equals(share.getPermission()))
                .orElse(false);
        
        if (!isOwner && !hasEditPermission) {
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
        
        // Check ownership or shared access
        boolean isOwner = fileEntity.getOwner().getId().equals(user.getId());
        boolean isShared = fileShareRepository.existsByFileAndSharedWith(fileEntity, user);
        
        if (!isOwner && !isShared) {
            log.warn("User {} attempted to access file {} without permission", username, fileId);
            throw new AccessDeniedException("You don't have permission to access this file");
        }
        
        try {
            // If file is stored on Cloudinary, redirect to Cloudinary URL
            if (fileEntity.getCloudinaryUrl() != null) {
                log.info("File is on Cloudinary: {}", fileEntity.getCloudinaryUrl());
                return new UrlResource(fileEntity.getCloudinaryUrl());
            }
            
            // Fallback to local storage (for old files)
            Path filePath = Paths.get(fileEntity.getFilePath()).normalize();
            
            log.info("Attempting to load file from local path: {}", filePath);
            
            String ownerIdStr = fileEntity.getOwner().getId().toString();
            Path expectedBasePath = Paths.get(BASE_UPLOAD_DIR, ownerIdStr).toAbsolutePath().normalize();
            Path absoluteFilePath = filePath.isAbsolute() ? filePath : filePath.toAbsolutePath().normalize();
            
            if (!absoluteFilePath.startsWith(expectedBasePath)) {
                log.error("Path traversal attempt detected. File path: {}, Expected base: {}", 
                         absoluteFilePath, expectedBasePath);
                throw new AccessDeniedException("Invalid file path");
            }
            
            if (!Files.exists(absoluteFilePath)) {
                log.error("File does not exist on disk: {}. This may be due to ephemeral storage.", absoluteFilePath);
                throw new ResourceNotFoundException("File not found on server. Files may be lost after server restart on free hosting.");
            }
            
            Resource resource = new UrlResource(absoluteFilePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                log.info("File loaded successfully: {}", absoluteFilePath);
                return resource;
            } else {
                log.error("File exists but not readable: {}", absoluteFilePath);
                throw new ResourceNotFoundException("File not readable");
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
        
        // Delete all shares first (to avoid foreign key constraint)
        fileShareRepository.deleteByFile(fileEntity);
        log.info("Deleted all shares for file: {}", fileId);
        
        // Delete file from Cloudinary if exists
        if (fileEntity.getCloudinaryPublicId() != null) {
            try {
                cloudinaryService.deleteFile(fileEntity.getCloudinaryPublicId());
                log.info("File deleted from Cloudinary: {}", fileEntity.getCloudinaryPublicId());
            } catch (IOException e) {
                log.error("Failed to delete file from Cloudinary: {}", fileEntity.getCloudinaryPublicId(), e);
                // Continue to delete from database even if Cloudinary delete fails
            }
        } else {
            // Delete from local disk (for old files)
            try {
                Path filePath = Paths.get(fileEntity.getFilePath()).normalize();
                Files.deleteIfExists(filePath);
                log.info("File deleted from disk: {}", filePath);
            } catch (IOException e) {
                log.error("Failed to delete file from disk: {}", fileEntity.getFilePath(), e);
            }
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
        
        // Check ownership or shared access
        boolean isOwner = fileEntity.getOwner().getId().equals(user.getId());
        boolean isShared = fileShareRepository.existsByFileAndSharedWith(fileEntity, user);
        
        if (!isOwner && !isShared) {
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
    
    @Transactional
    public FileDTO copySharedFileToMyFolder(Long sharedFileId, String username, String folderPath) {
        log.info("Copying shared file {} to user {}'s folder", sharedFileId, username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        FileEntity originalFile = fileRepository.findById(sharedFileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        
        // Check if user has access to this file
        boolean isShared = fileShareRepository.existsByFileAndSharedWith(originalFile, user);
        if (!isShared && !originalFile.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have access to this file");
        }
        
        // If file is on Cloudinary, just create a new reference (no need to copy)
        if (originalFile.getCloudinaryUrl() != null) {
            log.info("Creating reference to Cloudinary file for user");
            
            FileEntity newFile = FileEntity.builder()
                    .fileName(originalFile.getFileName())
                    .originalFileName(originalFile.getOriginalFileName())
                    .filePath(originalFile.getCloudinaryUrl())
                    .cloudinaryUrl(originalFile.getCloudinaryUrl())
                    .cloudinaryPublicId(originalFile.getCloudinaryPublicId())
                    .fileSize(originalFile.getFileSize())
                    .contentType(originalFile.getContentType())
                    .folderPath(folderPath)
                    .owner(user)
                    .isDeleted(false)
                    .build();
            
            FileEntity savedFile = fileRepository.save(newFile);
            log.info("File reference created successfully with id: {}", savedFile.getId());
            
            return convertToDTO(savedFile);
        }
        
        // Fallback: Copy from local storage (for old files)
        try {
            Path userDir = Paths.get(BASE_UPLOAD_DIR, user.getId().toString());
            if (folderPath != null && !folderPath.isEmpty()) {
                userDir = userDir.resolve(folderPath);
            }
            Files.createDirectories(userDir);
            
            String originalFileName = originalFile.getOriginalFileName();
            String fileExtension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = originalFileName.substring(dotIndex);
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            Path newFilePath = userDir.resolve(uniqueFileName);
            
            Path originalPath = Paths.get(originalFile.getFilePath()).toAbsolutePath().normalize();
            
            if (!Files.exists(originalPath)) {
                throw new FileStorageException("Original file not found on disk. File may have been lost due to server restart.");
            }
            
            Files.copy(originalPath, newFilePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File copied from {} to {}", originalPath, newFilePath);
            
            FileEntity newFile = FileEntity.builder()
                    .fileName(uniqueFileName)
                    .originalFileName(originalFileName)
                    .filePath(newFilePath.toString())
                    .fileSize(originalFile.getFileSize())
                    .contentType(originalFile.getContentType())
                    .folderPath(folderPath)
                    .owner(user)
                    .isDeleted(false)
                    .build();
            
            FileEntity savedFile = fileRepository.save(newFile);
            log.info("File copied successfully with id: {}", savedFile.getId());
            
            return convertToDTO(savedFile);
            
        } catch (IOException e) {
            log.error("Failed to copy file", e);
            throw new FileStorageException("Failed to copy file to your folder", e);
        }
    }
}
