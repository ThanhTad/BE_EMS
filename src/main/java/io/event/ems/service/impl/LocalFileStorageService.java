package io.event.ems.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import io.event.ems.exception.FileStorageException;
import io.event.ems.service.FileStorageService;

public class LocalFileStorageService implements FileStorageService {

    @Value("${file.storage.location}")
    private String storageLocation;

    @Value("${file.storage.base-url}")
    private String baseUrl;

    @Override
    public String storeFile(MultipartFile file, String directory) {
        try {
            Path dirPath = Paths.get(storageLocation, directory)
                    .toAbsolutePath().normalize();
            Files.createDirectories(dirPath);

            String filename = file.getOriginalFilename();
            String original = (filename != null) ? StringUtils.cleanPath(filename) : "";
            String ext = "";
            int idx = original.lastIndexOf('.');
            if (idx > 0)
                ext = original.substring(idx);
            String fileName = UUID.randomUUID() + ext;

            Path target = dirPath.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return String.format("%s%s%s", baseUrl, fileName);

        } catch (IOException e) {
            throw new FileStorageException("Không lưu được file, vui lòng thử lại.");
        }
    }

}
