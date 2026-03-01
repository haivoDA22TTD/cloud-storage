package com.example.demo.repository;

import com.example.demo.entity.Folder;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByOwner(User owner);
    Optional<Folder> findByPathAndOwner(String path, User owner);
    boolean existsByPathAndOwner(String path, User owner);
}
