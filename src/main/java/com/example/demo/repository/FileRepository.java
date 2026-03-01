package com.example.demo.repository;

import com.example.demo.entity.FileEntity;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByOwner(User owner);
    Optional<FileEntity> findByIdAndOwner(Long id, User owner);
    boolean existsByIdAndOwner(Long id, User owner);
}
