package com.demo.fileserver.controller;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

import com.demo.fileserver.model.MultipartInputStreamFileResource;
import com.demo.fileserver.model.ClusterMember;
import com.demo.fileserver.model.Table;
import com.demo.fileserver.repository.FilesRepository;
import com.demo.fileserver.storage.StorageFileNotFoundException;
import com.demo.fileserver.storage.StorageService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FileUploadController {

    @Value("${hz.instance.name}")
    private String instanceName;
    private final StorageService storageService;
    private final FilesRepository filesRepository;
    private final HazelcastInstance hazelcastInstance;

    @Autowired
    public FileUploadController(StorageService storageService, FilesRepository filesRepository, HazelcastInstance hazelcastInstance) {
        this.storageService = storageService;
        this.filesRepository = filesRepository;
        this.hazelcastInstance = hazelcastInstance;
    }

    @GetMapping("/")
    public String index(Model model) {

        model.addAttribute("files", filesRepository.findAll().stream()
                .map(fileEntity -> new Table(
                        MvcUriComponentsBuilder.fromMethodName(FileUploadController.class, "serveUUID", fileEntity.id()).build().toString(),
                        MvcUriComponentsBuilder.fromMethodName(FileUploadController.class, "serveFile", fileEntity.fileName()).build().toString(),
                        fileEntity))
                .collect(Collectors.toList()));

        return "uploadForm";
    }

    @DeleteMapping("/files/uuid/{id}")
    public String deleteByUUID(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        return filesRepository.findById(id)
                .map(fileEntity -> {
                    redirectAttributes.addFlashAttribute("message", "File with id: " + id + " was deleted");
                    if (fileEntity.node.equals(instanceName)) {
                        storageService.deleteByUUID(id);

                        return "redirect:/";
                    } else {
                        IMap<Object, Object> cluster = hazelcastInstance.getMap("cluster-info");
                        ClusterMember node = (ClusterMember) cluster.get(fileEntity.node);
                        new RestTemplate().exchange("http://" + node.address() + "/files/uuid/" + id, HttpMethod.DELETE, null, String.class);

                        return "redirect:/";
                    }
                })
                .orElseThrow(() -> new StorageFileNotFoundException("File not found"));
    }

    @GetMapping("/files/uuid/{id}")
    @ResponseBody
    public ResponseEntity<Resource> serveUUID(@PathVariable UUID id) {
        return filesRepository.findById(id)
                .map(fileEntity -> {
                    if (fileEntity.node.equals(instanceName)) {
                        Resource file = storageService.loadAsResource(fileEntity);
                        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
                    } else {
                        IMap<Object, Object> cluster = hazelcastInstance.getMap("cluster-info");
                        ClusterMember node = (ClusterMember) cluster.get(fileEntity.node);
                        return new RestTemplate().exchange("http://" + node.address() + "/files/uuid/" + id, HttpMethod.GET, null, Resource.class);
                    }
                })
                .orElseThrow(() -> new StorageFileNotFoundException("File not found"));
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        return filesRepository.findFirstByFileName(filename)
                .map(fileEntity -> {
                    if (fileEntity.node.equals(instanceName)) {
                        Resource file = storageService.loadAsResource(fileEntity);
                        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
                    } else {
                        IMap<Object, Object> cluster = hazelcastInstance.getMap("cluster-info");
                        ClusterMember node = (ClusterMember) cluster.get(fileEntity.node);
                        return new RestTemplate().exchange("http://" + node.address() + "/files/" + filename, HttpMethod.GET, null, Resource.class);
                    }
                })
                .orElseThrow(() -> new StorageFileNotFoundException("File not found"));
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        IMap<Object, Object> cluster = hazelcastInstance.getMap("cluster-info");
        ClusterMember node = (ClusterMember) cluster.get(instanceName);

        if (node.freeSpace() > file.getSize()) {
            storageService.save(file);
        } else {
            cluster.entrySet().stream()
                    .map(e -> (ClusterMember) e.getValue())
                    .filter(e -> e.freeSpace() > file.getSize())
                    .findFirst()
                    .map(next -> {
                        try {
                            LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
                            map.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
                            HttpHeaders headers = new HttpHeaders();
                            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
                            new RestTemplate().exchange("http://" + next.address() + "/upload", HttpMethod.POST, requestEntity, String.class);

                            return "redirect:/";
                        } catch (IOException e) {
                            throw new StorageFileNotFoundException("Cant proxy send file to node " + next.name());
                        }
                    })
                    .orElseThrow(() -> new StorageFileNotFoundException("Out of free space on all nodes"));
        }
        redirectAttributes.addFlashAttribute("message", "File is saved: " + file.getOriginalFilename());

        return "redirect:/";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException e) {
        return ResponseEntity.notFound().build();
    }
}
