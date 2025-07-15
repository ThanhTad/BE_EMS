package io.event.ems.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeFile(MultipartFile file, String destinationPath);

    void deleteFile(String fileUrl) throws Exception;

}
