package com.example.server.utils;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class MinioUtils {

    @Autowired(required = false)
    private MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${app.storage.mode:minio}")
    private String storageMode;

    @Value("${app.storage.local-root:./demo-storage}")
    private String localRoot;

    public String uploadFile(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID() + suffix;

        if (isLocalMode()) {
            Path storageDir = ensureLocalStorageDir();
            Path targetFile = storageDir.resolve(newFilename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return targetFile.toAbsolutePath().toString();
        }

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(newFilename)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }

        return endpoint + "/" + bucketName + "/" + newFilename;
    }

    public void removeFile(String fileUrl) {
        try {
            if (isLocalMode()) {
                Files.deleteIfExists(Path.of(fileUrl));
                return;
            }

            String objectName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            System.err.println("Storage delete failed: " + e.getMessage());
        }
    }

    public String uploadLocalFile(java.io.File file) throws Exception {
        if (isLocalMode()) {
            Path storageDir = ensureLocalStorageDir();
            Path targetFile = storageDir.resolve(file.getName());
            Files.copy(file.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            return targetFile.toAbsolutePath().toString();
        }

        try (java.io.FileInputStream inputStream = new java.io.FileInputStream(file)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(file.getName())
                            .stream(inputStream, file.length(), -1)
                            .contentType("video/mp4")
                            .build()
            );
        }

        return endpoint + "/" + bucketName + "/" + file.getName();
    }

    private boolean isLocalMode() {
        return "local".equalsIgnoreCase(storageMode);
    }

    private Path ensureLocalStorageDir() throws Exception {
        Path storageDir = Path.of(localRoot).toAbsolutePath().normalize();
        Files.createDirectories(storageDir);
        return storageDir;
    }
}
