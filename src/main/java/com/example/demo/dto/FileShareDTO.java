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
public class FileShareDTO {
    private Long id;
    private Long fileId;
    private String fileName;
    private String ownerUsername;
    private String sharedWithUsername;
    private String permission;
    private LocalDateTime sharedAt;
}
