package com.example.demo.dto;

import lombok.Data;

@Data
public class ShareFileRequest {
    private Long fileId;
    private String sharedWithUsername;
    private String permission; // "view" or "edit"
}
