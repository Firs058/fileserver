package com.demo.fileserver.controller;

import java.util.UUID;
import java.util.stream.Collectors;

import com.demo.fileserver.model.Table;
import com.demo.fileserver.repository.FilesRepository;
import com.demo.fileserver.storage.StorageFileNotFoundException;
import com.demo.fileserver.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FileUploadController {

    private final StorageService storageService;
    private final FilesRepository filesRepository;

    @Autowired
    public FileUploadController(StorageService storageService, FilesRepository filesRepository) {
        this.storageService = storageService;
        this.filesRepository = filesRepository;
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) {

        model.addAttribute("files", filesRepository.findAll().stream()
                .map(fileEntity -> new Table(
                        MvcUriComponentsBuilder.fromMethodName(FileUploadController.class, "serveUUID", fileEntity.id()).build().toString(),
                        MvcUriComponentsBuilder.fromMethodName(FileUploadController.class, "serveFile", fileEntity.fileName()).build().toString(),
                        fileEntity))
                .collect(Collectors.toList()));

        return "uploadForm";
    }

    @DeleteMapping("/files}")
    public String deleteAll(RedirectAttributes redirectAttributes) {
        storageService.deleteAll();
        redirectAttributes.addFlashAttribute("message", "Files was deleted");

        return "redirect:/";
    }

    @DeleteMapping("/files/uuid/{id}")
    public String deleteByUUID(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        storageService.deleteByUUID(id);
        redirectAttributes.addFlashAttribute("message", "File with id: " + id + " was deleted");

        return "redirect:/";
    }

    @GetMapping("/files/uuid/{id}")
    @ResponseBody
    public ResponseEntity<Resource> serveUUID(@PathVariable UUID id) {

        Resource file = storageService.loadAsResourceByUUID(id);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResourceByFilename(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        storageService.save(file);
        redirectAttributes.addFlashAttribute("message", "File is saved: " + file.getOriginalFilename());

        return "redirect:/";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException e) {
        return ResponseEntity.notFound().build();
    }

}
