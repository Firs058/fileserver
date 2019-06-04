package com.demo.fileserver.repository;


import com.demo.fileserver.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * @author firs
 */
@Repository
public interface FilesRepository extends JpaRepository<FileEntity, UUID> {
    Optional<FileEntity> findByFileName(String filename);
}
