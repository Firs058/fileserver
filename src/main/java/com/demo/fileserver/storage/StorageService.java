package com.demo.fileserver.storage;

import com.demo.fileserver.entity.FileEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface StorageService {

    void init();

    void save(MultipartFile file);

    Resource loadAsResource(FileEntity fileEntity);

    void deleteByUUID(UUID id);
}
