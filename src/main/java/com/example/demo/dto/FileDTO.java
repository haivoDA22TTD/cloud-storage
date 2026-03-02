package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDTO {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private String fileSizeFormatted;
    private String folderPath;
    private Boolean isDeleted;
    private Long ownerId;
    private String ownerUsername;
    private LocalDateTime uploadedAt;
    private LocalDateTime deletedAt;
    private Long folderId;
    private String folderName;
    private String sharedPermission; // "view" or "edit" if shared
}
