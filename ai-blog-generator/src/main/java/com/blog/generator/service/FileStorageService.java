package com.blog.generator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FileStorageService {

    @Value("${file.storage.path:generated-articles}")
    private String storagePath;

    public String saveArticle(String content, int articleNumber) throws IOException {
        File directory = new File(storagePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String fileName = String.format("article_%d_%s.txt", articleNumber, timestamp);

        Path filePath = Paths.get(storagePath, fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            writer.write(content);
        }

        return filePath.toString();
    }

    public String saveArticleWithBothVersions(String original, String replaced, int articleNumber) throws IOException {
        File directory = new File(storagePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String fileName = String.format("article_%d_%s.txt", articleNumber, timestamp);

        Path filePath = Paths.get(storagePath, fileName);

        StringBuilder content = new StringBuilder();
        content.append("==================== 원본 원고 ====================\n\n");
        content.append(original);
        content.append("\n\n\n");
        content.append("==================== 치환된 원고 ====================\n\n");
        content.append(replaced);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            writer.write(content.toString());
        }

        return filePath.toString();
    }
}
