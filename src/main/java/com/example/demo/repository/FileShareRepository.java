package com.example.demo.repository;

import com.example.demo.entity.FileEntity;
import com.example.demo.entity.FileShare;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileShareRepository extends JpaRepository<FileShare, Long> {
    
    List<FileShare> findBySharedWith(User user);
    
    List<FileShare> findByFile(FileEntity file);
    
    Optional<FileShare> findByFileAndSharedWith(FileEntity file, User user);
    
    boolean existsByFileAndSharedWith(FileEntity file, User user);
}
