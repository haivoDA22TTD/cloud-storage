package com.example.demo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {
    
    private final Cloudinary cloudinary;
    
    /**
     * Upload file to Cloudinary
     * @param file MultipartFile to upload
     * @param folder Folder path in Cloudinary (e.g., "user123/documents")
     * @return Map containing upload result with url, public_id, etc.
     */
    public Map<String, Object> uploadFile(MultipartFile file, String folder) throws IOException {
        log.info("Uploading file to Cloudinary: {} to folder: {}", file.getOriginalFilename(), folder);
        
        Map<String, Object> uploadParams = ObjectUtils.asMap(
            "folder", folder,
            "resource_type", "auto", // auto-detect file type
            "use_filename", true,
            "unique_filename", true
        );
        
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
        
        log.info("File uploaded successfully. URL: {}", uploadResult.get("secure_url"));
        return uploadResult;
    }
    
    /**
     * Delete file from Cloudinary
     * @param publicId Public ID of the file (returned from upload)
     */
    public void deleteFile(String publicId) throws IOException {
        log.info("Deleting file from Cloudinary: {}", publicId);
        
        Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        
        log.info("File deleted. Result: {}", result.get("result"));
    }
    
    /**
     * Get file URL from public ID
     * @param publicId Public ID of the file
     * @return Secure URL of the file
     */
    public String getFileUrl(String publicId) {
        return cloudinary.url().secure(true).generate(publicId);
    }
}
