package com.jialli.first_ai_project.aiagent.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileStoreService {
    String save(MultipartFile file) throws IOException;
    Path resolve(String fileName);

}
