package io.event.ems.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import io.event.ems.exception.FileStorageException;
import io.event.ems.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.storage.location}")
    private String storageLocation;

    @Value("${file.storage.base-url}")
    private String baseUrl;

    @Override
    public String storeFile(MultipartFile file, String directory) {
        try {
            // Tạo thư mục lưu file nếu chưa tồn tại
            Path dirPath = Paths.get(storageLocation, directory)
                    .toAbsolutePath().normalize();
            Files.createDirectories(dirPath);

            // Xử lý tên file và phần mở rộng
            String filename = file.getOriginalFilename();
            String original = (filename != null) ? StringUtils.cleanPath(filename) : "";
            String ext = "";
            int idx = original.lastIndexOf('.');
            if (idx > 0) {
                ext = original.substring(idx);
            } else {
                // Nếu không lấy được ext từ tên file, lấy từ contentType
                String contentType = file.getContentType();
                if ("image/jpeg".equals(contentType))
                    ext = ".jpg";
                else if ("image/png".equals(contentType))
                    ext = ".png";
                else if ("image/gif".equals(contentType))
                    ext = ".gif";
                // Có thể đặt mặc định là .png nếu muốn
            }
            String fileName = UUID.randomUUID() + ext;

            // Lưu file vật lý
            Path target = dirPath.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // Trả về đường dẫn đầy đủ, có cả subdirectory
            String url = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            String directoryPart = (directory == null || directory.isEmpty()) ? ""
                    : (directory.endsWith("/") ? directory : directory + "/");
            return url + directoryPart + fileName;

        } catch (IOException e) {
            throw new FileStorageException("Không lưu được file, vui lòng thử lại.");
        }
    }

    @Override
    public String deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                log.warn("Đường dẫn file rỗng");
                return null;
            }

            String fileRelativePath = extractRelativePathFromUrl(fileUrl);
            if (fileRelativePath == null) {
                log.warn("Không thể trích xuất đường dẫn file từ URL: {}", fileUrl);
                return null;
            }

            Path filePath = Paths.get(storageLocation).resolve(fileRelativePath).normalize();

            if (!Files.exists(filePath)) {
                log.warn("File không tồn tại: {}", filePath);
                return null;
            }

            if (!Files.isRegularFile(filePath)) {
                log.warn("Đường dẫn không phải là file: {}", filePath);
                return null;
            }

            Files.delete(filePath);
            log.info("Đã xóa file: {}", filePath);
            return fileRelativePath;

        } catch (IOException e) {
            log.error("Lỗi khi xóa file: {}", fileUrl, e);
            throw new FileStorageException("Không xóa được file, vui lòng thử lại.");
        }
    }

    /**
     * Trả về relative path (bao gồm cả subdirectory) từ URL, trừ đi baseUrl
     */
    private String extractRelativePathFromUrl(String fileUrl) {
        try {
            // baseUrl luôn kết thúc bằng '/'
            String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            if (!fileUrl.startsWith(normalizedBaseUrl)) {
                log.warn("URL không hợp lệ, không chứa base URL: {}", fileUrl);
                return null;
            }
            // Lấy phần sau baseUrl (bao gồm thư mục con)
            return fileUrl.substring(normalizedBaseUrl.length());
        } catch (Exception e) {
            log.error("Lỗi khi trích xuất đường dẫn file từ URL: {}", fileUrl, e);
            return null;
        }
    }

}