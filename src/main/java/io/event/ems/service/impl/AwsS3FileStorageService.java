package io.event.ems.service.impl;

import io.event.ems.exception.FileStorageException;
import io.event.ems.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class AwsS3FileStorageService implements FileStorageService {

    private final S3Client s3Client;

    private final String bucketName;

    private static final Set<String> ALLOWED_FILE_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");

    private static final Map<String, String> MIME_TYPE_TO_EXTENSION = Map.of("image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp");

    public AwsS3FileStorageService(S3Client s3Client, @Value("${aws.s3.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public String storeFile(MultipartFile file, String destinationPath) {
        String detectedContentType;
        String fileExtension;

        // 1. Xác thực và lấy extension an toàn
        try {
            // Sử dụng Tika để phát hiện loại file thực sự từ nội dung
            detectedContentType = new Tika().detect(file.getInputStream());

            // 2. Kiểm tra xem loại file có được phép không
            if (!ALLOWED_FILE_TYPES.contains(detectedContentType)) {
                throw new FileStorageException("File type not allowed. Allowed types: " + ALLOWED_FILE_TYPES);
            }
            // 3. Lấy extension an toàn từ map
            fileExtension = MIME_TYPE_TO_EXTENSION.get(detectedContentType);
            if (fileExtension == null) {
                throw new FileStorageException("File cannot be stored. Unknown file type: " + detectedContentType);
            }
        } catch (IOException e) {
            throw new FileStorageException("Cannot read downloaded file: " + e.getMessage());
        }

        // 4. Tạo key và tiến hành upload
        String objectKey = destinationPath + "/" + UUID.randomUUID() + fileExtension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(detectedContentType)
                .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(objectKey)).toExternalForm();
        } catch (IOException e) {
            throw new FileStorageException("Cannot upload file: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        try {
            URL url = new URL(fileUrl);
            String key = url.getPath().substring(1);
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            System.err.println("Không thể xóa file từ S3: " + fileUrl + ". Lỗi: " + e.getMessage());
        }
    }

}