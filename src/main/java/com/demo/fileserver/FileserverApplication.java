package com.demo.fileserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FileserverApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileserverApplication.class, args);
    }

}
