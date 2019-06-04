package com.demo.fileserver.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;

public interface StorageService {

    void init();

    void save(MultipartFile file);

    Path loadByFilename(String filename);

    Path loadById(UUID id);

    Resource loadAsResourceByUUID(UUID id);

    Resource loadAsResourceByFilename(String filename);

    void deleteAll();

    void deleteByUUID(UUID id);
}
