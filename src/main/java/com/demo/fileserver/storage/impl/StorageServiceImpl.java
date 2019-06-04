package com.demo.fileserver.storage.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

import com.demo.fileserver.entity.FileEntity;
import com.demo.fileserver.repository.FilesRepository;
import com.demo.fileserver.storage.StorageException;
import com.demo.fileserver.storage.StorageFileNotFoundException;
import com.demo.fileserver.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageServiceImpl implements StorageService {

    @Value("${paths.upload}")
    private Path uploadPath;
    private final FilesRepository filesRepository;

    @Autowired
    public StorageServiceImpl(FilesRepository filesRepository) {
        this.filesRepository = filesRepository;
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new StorageException("Directory create error", e);
        }
    }

    @Override
    public void save(MultipartFile multipartFile) {
        String filename = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
        try {
            if (multipartFile.isEmpty()) {
                throw new StorageException("File is empty " + filename);
            }
            if (filename.contains("..")) {
                throw new StorageException("Wrong path " + filename);
            }
            try (InputStream inputStream = multipartFile.getInputStream()) {
                FileEntity fileEntity = new FileEntity()
                        .fileName(multipartFile.getOriginalFilename())
                        .path(uploadPath.toString())
                        .mimeType(multipartFile.getContentType())
                        .fileSize(multipartFile.getSize());
                filesRepository.save(fileEntity);
                Files.copy(inputStream, this.uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("File save error" + filename, e);
        }
    }

    @Override
    public Path loadByFilename(String filename) {
        return filesRepository.findByFileName(filename)
                .map(u -> uploadPath.resolve(filename))
                .orElseThrow(() -> new StorageException("Files read error"));
    }


    @Override
    public Path loadById(UUID id) {
        return filesRepository.findById(id)
                .map(u -> uploadPath.resolve(u.fileName()))
                .orElseThrow(() -> new StorageException("Files read error"));
    }

    @Override
    public Resource loadAsResourceByUUID(UUID id) {
        return loadFile(loadById(id));
    }

    @Override
    public Resource loadAsResourceByFilename(String filename) {
        return loadFile(loadByFilename(filename));
    }

    @Override
    public void deleteAll() {
        filesRepository.findAll().forEach(this::deleteFile);
    }

    @Override
    public void deleteByUUID(UUID id) {
        filesRepository.findById(id).ifPresent(this::deleteFile);
    }

    private Resource loadFile(Path path) {
        try {
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException("File read error: " + path);
            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("File read error: " + path);
        }
    }

    private void deleteFile(FileEntity fileEntity){
        File fileToDelete = new File(Paths.get(fileEntity.path(), fileEntity.fileName()).toUri());
        boolean deleted = fileToDelete.delete();
        if (deleted) {
            filesRepository.delete(fileEntity);
        }
    }
}
